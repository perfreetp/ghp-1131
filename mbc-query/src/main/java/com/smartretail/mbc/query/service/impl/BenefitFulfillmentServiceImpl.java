package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.entity.IdempotentRecord;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.mapper.IdempotentRecordMapper;
import com.smartretail.mbc.common.enums.OrderStatusEnum;
import com.smartretail.mbc.common.enums.RiskLevelEnum;
import com.smartretail.mbc.common.enums.RiskSceneEnum;
import com.smartretail.mbc.common.enums.StoreTaskTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.member.entity.StoreTask;
import com.smartretail.mbc.member.mapper.StoreTaskMapper;
import com.smartretail.mbc.order.dto.BenefitFulfillmentQueryDTO;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.order.service.BenefitFulfillmentService;
import com.smartretail.mbc.order.vo.BenefitFulfillmentVO;
import com.smartretail.mbc.order.vo.FulfillmentCSVO;
import com.smartretail.mbc.order.vo.FulfillmentItemVO;
import com.smartretail.mbc.order.vo.FulfillmentRiskVO;
import com.smartretail.mbc.query.entity.RiskRecord;
import com.smartretail.mbc.query.mapper.RiskRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitFulfillmentServiceImpl implements BenefitFulfillmentService {

    private final ConsumeOrderMapper consumeOrderMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final RiskRecordMapper riskRecordMapper;
    private final StoreTaskMapper storeTaskMapper;

    private static final int STEP_TYPE_BENEFIT_LOCK = 1;
    private static final int STEP_TYPE_BENEFIT_CONFIRM = 2;
    private static final int STEP_TYPE_BENEFIT_RETURN = 3;
    private static final int STEP_TYPE_ORDER_PAY = 4;
    private static final int STEP_TYPE_ORDER_COMPLETE = 5;
    private static final int STEP_TYPE_ORDER_REFUND = 6;

    private static final int STEP_STATUS_PENDING = 0;
    private static final int STEP_STATUS_PROCESSING = 1;
    private static final int STEP_STATUS_COMPLETED = 2;
    private static final int STEP_STATUS_FAILED = 3;

    private static final int USE_STATUS_LOCKED = 1;
    private static final int USE_STATUS_CONFIRMED = 2;
    private static final int USE_STATUS_RETURNED = 3;

    private static final int OPERATOR_TYPE_MANUAL_REPLAY = 1;
    private static final int OPERATOR_TYPE_MANUAL_MARK_FAIL = 2;

    @Override
    public BenefitFulfillmentVO getFulfillmentStatus(BenefitFulfillmentQueryDTO dto) {
        if (!StringUtils.hasText(dto.getOrderNo())
                && dto.getMemberId() == null
                && !StringUtils.hasText(dto.getRefundNo())) {
            throw new BusinessException("订单号、会员ID、退款单号至少填一个");
        }

        ConsumeOrder order = findOrder(dto);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        BenefitFulfillmentVO vo = new BenefitFulfillmentVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStoreCode(order.getStoreCode());
        vo.setStoreName(order.getStoreName());
        vo.setPayTime(order.getPayTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setRefundTime(order.getRefundTime());

        BigDecimal couponSavings = order.getCouponAmount() != null ? order.getCouponAmount() : BigDecimal.ZERO;
        BigDecimal pointSavings = order.getPointAmount() != null ? order.getPointAmount() : BigDecimal.ZERO;
        BigDecimal levelSavings = order.getLevelDiscount() != null ? order.getLevelDiscount() : BigDecimal.ZERO;
        BigDecimal totalSavings = couponSavings.add(pointSavings).add(levelSavings);
        vo.setCouponSavings(couponSavings);
        vo.setPointSavings(pointSavings);
        vo.setLevelSavings(levelSavings);
        vo.setTotalSavings(totalSavings);

        List<FulfillmentItemVO> items = buildFulfillmentItems(order);
        vo.setItems(items);

        FulfillmentCSVO csInfo = buildCustomerServiceInfo(order.getOrderNo());
        vo.setCustomerService(csInfo);

        List<FulfillmentRiskVO> risks = buildRiskRecords(order.getOrderNo());
        vo.setRisks(risks);

        return vo;
    }

    @Override
    public List<BenefitFulfillmentVO> getMemberFulfillmentList(Long memberId, Integer pageNum, Integer pageSize) {
        if (memberId == null) {
            throw new BusinessException("会员ID不能为空");
        }

        LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumeOrder::getMemberId, memberId)
                .orderByDesc(ConsumeOrder::getCreateTime);

        Page<ConsumeOrder> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        IPage<ConsumeOrder> orderPage = consumeOrderMapper.selectPage(page, wrapper);

        return orderPage.getRecords().stream()
                .map(this::buildSimpleFulfillmentVO)
                .collect(Collectors.toList());
    }

    private ConsumeOrder findOrder(BenefitFulfillmentQueryDTO dto) {
        if (StringUtils.hasText(dto.getOrderNo())) {
            return consumeOrderMapper.selectByOrderNo(dto.getOrderNo());
        }
        if (StringUtils.hasText(dto.getRefundNo())) {
            LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConsumeOrder::getRefundNo, dto.getRefundNo());
            return consumeOrderMapper.selectOne(wrapper);
        }
        if (dto.getMemberId() != null) {
            LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConsumeOrder::getMemberId, dto.getMemberId())
                    .orderByDesc(ConsumeOrder::getCreateTime)
                    .last("LIMIT 1");
            return consumeOrderMapper.selectOne(wrapper);
        }
        return null;
    }

    private List<FulfillmentItemVO> buildFulfillmentItems(ConsumeOrder order) {
        List<FulfillmentItemVO> items = new ArrayList<>();

        List<BenefitUseLog> useLogs = benefitUseLogMapper.selectByOrderNo(order.getOrderNo());

        for (BenefitUseLog useLog : useLogs) {
            if (useLog.getLockTime() != null) {
                FulfillmentItemVO item = new FulfillmentItemVO();
                item.setStepType(STEP_TYPE_BENEFIT_LOCK);
                item.setStepName(getBenefitTypeName(useLog.getBenefitType()) + "锁定");
                item.setStatus(STEP_STATUS_COMPLETED);
                item.setStatusName("已完成");
                item.setDescription(getBenefitTypeDescription(useLog.getBenefitType(), useLog));
                item.setAmount(useLog.getBenefitValue());
                item.setPoints(useLog.getUsedPoints());
                item.setOperator(useLog.getOperator());
                item.setOperateTime(useLog.getLockTime());
                item.setRemark(useLog.getRemark());
                items.add(item);
            }
        }

        if (order.getPayTime() != null) {
            FulfillmentItemVO item = new FulfillmentItemVO();
            item.setStepType(STEP_TYPE_ORDER_PAY);
            item.setStepName("订单支付");
            item.setStatus(STEP_STATUS_COMPLETED);
            item.setStatusName("已完成");
            item.setDescription("订单支付成功");
            item.setAmount(order.getPayAmount());
            item.setOperateTime(order.getPayTime());
            items.add(item);
        } else if (order.getOrderStatus() != null && order.getOrderStatus() == 0) {
            FulfillmentItemVO item = new FulfillmentItemVO();
            item.setStepType(STEP_TYPE_ORDER_PAY);
            item.setStepName("订单支付");
            item.setStatus(STEP_STATUS_PENDING);
            item.setStatusName("待处理");
            item.setDescription("等待支付");
            items.add(item);
        }

        for (BenefitUseLog useLog : useLogs) {
            if (useLog.getConfirmTime() != null) {
                FulfillmentItemVO item = new FulfillmentItemVO();
                item.setStepType(STEP_TYPE_BENEFIT_CONFIRM);
                item.setStepName(getBenefitTypeName(useLog.getBenefitType()) + "核销");
                item.setStatus(STEP_STATUS_COMPLETED);
                item.setStatusName("已完成");
                item.setDescription(getBenefitTypeDescription(useLog.getBenefitType(), useLog));
                item.setAmount(useLog.getBenefitValue());
                item.setPoints(useLog.getUsedPoints());
                item.setOperator(useLog.getOperator());
                item.setOperateTime(useLog.getConfirmTime());
                item.setRemark(useLog.getRemark());
                items.add(item);
            }
        }

        if (order.getCompleteTime() != null) {
            FulfillmentItemVO item = new FulfillmentItemVO();
            item.setStepType(STEP_TYPE_ORDER_COMPLETE);
            item.setStepName("订单完成");
            item.setStatus(STEP_STATUS_COMPLETED);
            item.setStatusName("已完成");
            item.setDescription("订单已完成");
            item.setOperateTime(order.getCompleteTime());
            items.add(item);
        }

        for (BenefitUseLog useLog : useLogs) {
            if (useLog.getReturnTime() != null) {
                FulfillmentItemVO item = new FulfillmentItemVO();
                item.setStepType(STEP_TYPE_BENEFIT_RETURN);
                item.setStepName(getBenefitTypeName(useLog.getBenefitType()) + "返还");
                item.setStatus(STEP_STATUS_COMPLETED);
                item.setStatusName("已完成");
                item.setDescription(getBenefitTypeDescription(useLog.getBenefitType(), useLog));
                item.setAmount(useLog.getBenefitValue());
                item.setPoints(useLog.getUsedPoints());
                item.setOperator(useLog.getOperator());
                item.setOperateTime(useLog.getReturnTime());
                item.setRemark(useLog.getReturnReason());
                items.add(item);
            }
        }

        if (order.getRefundTime() != null) {
            FulfillmentItemVO item = new FulfillmentItemVO();
            item.setStepType(STEP_TYPE_ORDER_REFUND);
            item.setStepName("订单退款");
            item.setStatus(STEP_STATUS_COMPLETED);
            item.setStatusName("已完成");
            item.setDescription("订单已退款");
            item.setAmount(order.getRefundAmount());
            item.setOperateTime(order.getRefundTime());
            items.add(item);
        } else if (order.getOrderStatus() != null && order.getOrderStatus() == 4) {
            FulfillmentItemVO item = new FulfillmentItemVO();
            item.setStepType(STEP_TYPE_ORDER_REFUND);
            item.setStepName("订单退款");
            item.setStatus(STEP_STATUS_PROCESSING);
            item.setStatusName("处理中");
            item.setDescription("退款处理中");
            items.add(item);
        }

        items.sort(Comparator.comparing(item -> {
            LocalDateTime time = item.getOperateTime();
            return time != null ? time : LocalDateTime.MIN;
        }));

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setStepNo(i + 1);
        }

        return items;
    }

    private FulfillmentCSVO buildCustomerServiceInfo(String orderNo) {
        FulfillmentCSVO csVO = new FulfillmentCSVO();
        csVO.setHasCsIntervention(false);

        LocalDateTime latestTime = null;
        String csStaff = null;
        String csAction = null;
        String csResult = null;
        String csRemark = null;

        LambdaQueryWrapper<IdempotentRecord> idempotentWrapper = new LambdaQueryWrapper<>();
        idempotentWrapper.eq(IdempotentRecord::getBusinessNo, orderNo)
                .and(w -> w.eq(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_REPLAY)
                        .or()
                        .eq(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_MARK_FAIL))
                .orderByDesc(IdempotentRecord::getOperateTime)
                .last("LIMIT 1");
        IdempotentRecord idempotentRecord = idempotentRecordMapper.selectOne(idempotentWrapper);

        if (idempotentRecord != null && idempotentRecord.getOperateTime() != null) {
            latestTime = idempotentRecord.getOperateTime();
            csStaff = idempotentRecord.getOperator();
            csAction = getOperatorTypeName(idempotentRecord.getOperatorType());
            csResult = getProcessStatusName(idempotentRecord.getProcessStatus());
            csRemark = idempotentRecord.getRemark();
        }

        LambdaQueryWrapper<StoreTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(StoreTask::getBizId, orderNo)
                .eq(StoreTask::getStatus, 2)
                .orderByDesc(StoreTask::getHandleTime)
                .last("LIMIT 1");
        StoreTask storeTask = storeTaskMapper.selectOne(taskWrapper);

        if (storeTask != null && storeTask.getHandleTime() != null) {
            if (latestTime == null || storeTask.getHandleTime().isAfter(latestTime)) {
                latestTime = storeTask.getHandleTime();
                csStaff = storeTask.getHandler();
                csAction = getTaskTypeName(storeTask.getTaskType());
                csResult = storeTask.getHandleResult();
                csRemark = storeTask.getDescription();
            }
        }

        if (latestTime != null) {
            csVO.setHasCsIntervention(true);
            csVO.setCsStaff(csStaff);
            csVO.setCsTime(latestTime);
            csVO.setCsAction(csAction);
            csVO.setCsResult(csResult);
            csVO.setCsRemark(csRemark);
        }

        return csVO;
    }

    private List<FulfillmentRiskVO> buildRiskRecords(String orderNo) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getOrderNo, orderNo)
                .orderByDesc(RiskRecord::getCreateTime);
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);

        return records.stream()
                .map(this::convertToRiskVO)
                .collect(Collectors.toList());
    }

    private FulfillmentRiskVO convertToRiskVO(RiskRecord record) {
        FulfillmentRiskVO vo = new FulfillmentRiskVO();
        vo.setRecordNo(record.getRecordNo());
        vo.setRiskLevel(record.getRiskLevel());
        vo.setRiskLevelName(getRiskLevelName(record.getRiskLevel()));
        vo.setScene(record.getScene());
        vo.setSceneName(getSceneName(record.getScene()));
        vo.setHandleResult(record.getHandleResult());
        vo.setHandleResultName(getHandleResultName(record.getHandleResult()));
        vo.setHandleStaff(record.getHandleStaff());
        vo.setHandleTime(record.getHandleTime());
        vo.setHandleRemark(record.getHandleRemark());
        return vo;
    }

    private BenefitFulfillmentVO buildSimpleFulfillmentVO(ConsumeOrder order) {
        BenefitFulfillmentVO vo = new BenefitFulfillmentVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStoreCode(order.getStoreCode());
        vo.setStoreName(order.getStoreName());
        vo.setPayTime(order.getPayTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setRefundTime(order.getRefundTime());

        BigDecimal couponSavings = order.getCouponAmount() != null ? order.getCouponAmount() : BigDecimal.ZERO;
        BigDecimal pointSavings = order.getPointAmount() != null ? order.getPointAmount() : BigDecimal.ZERO;
        BigDecimal levelSavings = order.getLevelDiscount() != null ? order.getLevelDiscount() : BigDecimal.ZERO;
        vo.setCouponSavings(couponSavings);
        vo.setPointSavings(pointSavings);
        vo.setLevelSavings(levelSavings);
        vo.setTotalSavings(couponSavings.add(pointSavings).add(levelSavings));

        List<FulfillmentItemVO> items = buildFulfillmentItems(order);
        vo.setItems(items);

        FulfillmentCSVO csInfo = buildCustomerServiceInfo(order.getOrderNo());
        vo.setCustomerService(csInfo);

        return vo;
    }

    private String getOrderStatusName(Integer orderStatus) {
        if (orderStatus == null) {
            return "";
        }
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getCode().equals(orderStatus)) {
                return e.getName();
            }
        }
        return "未知";
    }

    private String getBenefitTypeName(Integer type) {
        if (type == null) {
            return "权益";
        }
        switch (type) {
            case 1: return "优惠券";
            case 2: return "积分抵扣";
            case 3: return "等级折扣";
            default: return "权益";
        }
    }

    private String getBenefitTypeDescription(Integer benefitType, BenefitUseLog useLog) {
        if (benefitType == null) {
            return "";
        }
        switch (benefitType) {
            case 1:
                return "优惠券抵扣 " + (useLog.getBenefitValue() != null ? useLog.getBenefitValue() : BigDecimal.ZERO) + " 元";
            case 2:
                return "使用 " + (useLog.getUsedPoints() != null ? useLog.getUsedPoints() : 0) + " 积分抵扣 "
                        + (useLog.getBenefitValue() != null ? useLog.getBenefitValue() : BigDecimal.ZERO) + " 元";
            case 3:
                return "等级折扣优惠 " + (useLog.getBenefitValue() != null ? useLog.getBenefitValue() : BigDecimal.ZERO) + " 元";
            default:
                return "";
        }
    }

    private String getOperatorTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case OPERATOR_TYPE_MANUAL_REPLAY: return "人工重放";
            case OPERATOR_TYPE_MANUAL_MARK_FAIL: return "人工标记失败";
            default: return "未知";
        }
    }

    private String getProcessStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 1: return "处理中";
            case 2: return "已完成";
            case 3: return "已失败";
            default: return "未知";
        }
    }

    private String getTaskTypeName(Integer type) {
        StoreTaskTypeEnum typeEnum = StoreTaskTypeEnum.getByCode(type);
        return typeEnum != null ? typeEnum.getName() : "未知";
    }

    private String getRiskLevelName(Integer level) {
        RiskLevelEnum levelEnum = RiskLevelEnum.getByCode(level);
        return levelEnum != null ? levelEnum.getName() : "未知";
    }

    private String getSceneName(Integer scene) {
        RiskSceneEnum sceneEnum = RiskSceneEnum.getByCode(scene);
        return sceneEnum != null ? sceneEnum.getName() : "未知";
    }

    private String getHandleResultName(Integer result) {
        if (result == null) {
            return "待处理";
        }
        switch (result) {
            case 1: return "放行";
            case 2: return "人工确认";
            case 3: return "拦截";
            default: return "未知";
        }
    }
}
