package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.dto.BenefitReturnDTO;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.entity.IdempotentRecord;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.mapper.IdempotentRecordMapper;
import com.smartretail.mbc.benefit.service.BenefitService;
import com.smartretail.mbc.common.enums.OrderStatusEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.query.dto.BenefitChainQueryDTO;
import com.smartretail.mbc.query.dto.IdempotentHandleDTO;
import com.smartretail.mbc.query.service.ExceptionHandleService;
import com.smartretail.mbc.query.vo.BenefitChainItemVO;
import com.smartretail.mbc.query.vo.BenefitChainVO;
import com.smartretail.mbc.query.vo.IdempotentRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionHandleServiceImpl implements ExceptionHandleService {

    private final IdempotentRecordMapper idempotentRecordMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final MemberMapper memberMapper;
    private final BenefitService benefitService;
    private final ObjectMapper objectMapper;

    private static final int PROCESS_STATUS_PROCESSING = 1;
    private static final int PROCESS_STATUS_COMPLETED = 2;
    private static final int PROCESS_STATUS_FAILED = 3;

    private static final int BIZ_TYPE_PAY_LOCK = 1;
    private static final int BIZ_TYPE_CONFIRM = 2;
    private static final int BIZ_TYPE_REFUND_RETURN = 3;

    private static final int OPERATOR_TYPE_SYSTEM = 0;
    private static final int OPERATOR_TYPE_MANUAL_REPLAY = 1;
    private static final int OPERATOR_TYPE_MANUAL_MARK_FAIL = 2;

    private static final int USE_STATUS_LOCKED = 1;
    private static final int USE_STATUS_CONFIRMED = 2;
    private static final int USE_STATUS_RETURNED = 3;

    @Override
    public BenefitChainVO getBenefitChain(BenefitChainQueryDTO dto) {
        if (!dto.hasAtLeastOne()) {
            throw new BusinessException("订单号和退款单号至少填一个");
        }

        String orderNo = dto.getOrderNo();
        String refundNo = dto.getRefundNo();

        BenefitChainVO chainVO = new BenefitChainVO();

        ConsumeOrder order = null;
        if (StringUtils.hasText(orderNo)) {
            order = consumeOrderMapper.selectByOrderNo(orderNo);
        }
        if (order == null && StringUtils.hasText(refundNo)) {
            LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConsumeOrder::getRefundNo, refundNo);
            order = consumeOrderMapper.selectOne(wrapper);
            if (order != null && !StringUtils.hasText(orderNo)) {
                orderNo = order.getOrderNo();
            }
        }

        if (order != null) {
            chainVO.setOrderNo(order.getOrderNo());
            chainVO.setOrderStatus(order.getOrderStatus());
            chainVO.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
            chainVO.setMemberId(order.getMemberId());
            chainVO.setLockTime(order.getPayTime());

            Member member = memberMapper.selectById(order.getMemberId());
            if (member != null) {
                chainVO.setMemberName(member.getName());
            }
        }

        if (StringUtils.hasText(orderNo)) {
            List<BenefitUseLog> logs = benefitUseLogMapper.selectByOrderNo(orderNo);
            List<BenefitChainItemVO> items = convertToChainItems(logs);
            chainVO.setBenefitLogs(items);

            if (!logs.isEmpty()) {
                BenefitUseLog firstLock = logs.stream()
                        .filter(l -> l.getLockTime() != null)
                        .min(Comparator.comparing(BenefitUseLog::getLockTime))
                        .orElse(null);
                if (firstLock != null && chainVO.getLockTime() == null) {
                    chainVO.setLockTime(firstLock.getLockTime());
                }

                BenefitUseLog lastConfirm = logs.stream()
                        .filter(l -> l.getConfirmTime() != null)
                        .max(Comparator.comparing(BenefitUseLog::getConfirmTime))
                        .orElse(null);
                if (lastConfirm != null) {
                    chainVO.setConfirmTime(lastConfirm.getConfirmTime());
                }

                BenefitUseLog lastReturn = logs.stream()
                        .filter(l -> l.getReturnTime() != null)
                        .max(Comparator.comparing(BenefitUseLog::getReturnTime))
                        .orElse(null);
                if (lastReturn != null) {
                    chainVO.setRefundTime(lastReturn.getReturnTime());
                }
            }
        } else {
            chainVO.setBenefitLogs(new ArrayList<>());
        }

        List<IdempotentRecord> idempotentRecords = new ArrayList<>();
        if (StringUtils.hasText(orderNo)) {
            LambdaQueryWrapper<IdempotentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IdempotentRecord::getBusinessNo, orderNo);
            idempotentRecords.addAll(idempotentRecordMapper.selectList(wrapper));
        }
        if (StringUtils.hasText(refundNo)) {
            LambdaQueryWrapper<IdempotentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IdempotentRecord::getBusinessNo, refundNo);
            idempotentRecords.addAll(idempotentRecordMapper.selectList(wrapper));
        }

        List<IdempotentRecordVO> recordVOs = idempotentRecords.stream()
                .map(this::convertToIdempotentRecordVO)
                .collect(Collectors.toList());
        chainVO.setIdempotentRecords(recordVOs);

        return chainVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdempotentRecordVO handleIdempotent(IdempotentHandleDTO dto) {
        IdempotentRecord record = idempotentRecordMapper.selectById(dto.getId());
        if (record == null) {
            throw new BusinessException("幂等记录不存在");
        }

        if (dto.getAction() == 1) {
            if (record.getProcessStatus() != PROCESS_STATUS_PROCESSING
                    && record.getProcessStatus() != PROCESS_STATUS_FAILED) {
                throw new BusinessException("当前状态不支持人工重放");
            }
            return doManualReplay(record, dto);
        } else if (dto.getAction() == 2) {
            if (record.getProcessStatus() != PROCESS_STATUS_PROCESSING) {
                throw new BusinessException("当前状态不支持标记失败");
            }
            return doMarkFail(record, dto);
        } else {
            throw new BusinessException("不支持的操作类型");
        }
    }

    private IdempotentRecordVO doManualReplay(IdempotentRecord record, IdempotentHandleDTO dto) {
        Integer businessType = record.getBusinessType();
        String requestParam = record.getRequestParam();

        try {
            if (BIZ_TYPE_PAY_LOCK == businessType) {
                BenefitLockDTO lockDTO = objectMapper.readValue(requestParam, BenefitLockDTO.class);
                benefitService.lockBenefits(lockDTO);
            } else if (BIZ_TYPE_CONFIRM == businessType) {
                BenefitConfirmDTO confirmDTO = objectMapper.readValue(requestParam, BenefitConfirmDTO.class);
                benefitService.confirmBenefits(confirmDTO);
            } else if (BIZ_TYPE_REFUND_RETURN == businessType) {
                BenefitReturnDTO returnDTO = objectMapper.readValue(requestParam, BenefitReturnDTO.class);
                benefitService.returnBenefits(returnDTO);
            } else {
                throw new BusinessException("未知的业务类型: " + businessType);
            }

            LambdaUpdateWrapper<IdempotentRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(IdempotentRecord::getId, record.getId())
                    .set(IdempotentRecord::getProcessStatus, PROCESS_STATUS_COMPLETED)
                    .set(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_REPLAY)
                    .set(IdempotentRecord::getOperator, dto.getOperator())
                    .set(IdempotentRecord::getRetryCount, record.getRetryCount() + 1)
                    .set(IdempotentRecord::getOperateTime, LocalDateTime.now())
                    .set(IdempotentRecord::getRemark, dto.getRemark());
            idempotentRecordMapper.update(null, wrapper);
        } catch (BusinessException e) {
            LambdaUpdateWrapper<IdempotentRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(IdempotentRecord::getId, record.getId())
                    .set(IdempotentRecord::getProcessStatus, PROCESS_STATUS_FAILED)
                    .set(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_REPLAY)
                    .set(IdempotentRecord::getOperator, dto.getOperator())
                    .set(IdempotentRecord::getRetryCount, record.getRetryCount() + 1)
                    .set(IdempotentRecord::getResultMsg, e.getMessage())
                    .set(IdempotentRecord::getOperateTime, LocalDateTime.now())
                    .set(IdempotentRecord::getRemark, dto.getRemark());
            idempotentRecordMapper.update(null, wrapper);
            throw e;
        } catch (Exception e) {
            LambdaUpdateWrapper<IdempotentRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(IdempotentRecord::getId, record.getId())
                    .set(IdempotentRecord::getProcessStatus, PROCESS_STATUS_FAILED)
                    .set(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_REPLAY)
                    .set(IdempotentRecord::getOperator, dto.getOperator())
                    .set(IdempotentRecord::getRetryCount, record.getRetryCount() + 1)
                    .set(IdempotentRecord::getResultMsg, e.getMessage())
                    .set(IdempotentRecord::getOperateTime, LocalDateTime.now())
                    .set(IdempotentRecord::getRemark, dto.getRemark());
            idempotentRecordMapper.update(null, wrapper);
            throw new BusinessException("人工重放执行失败: " + e.getMessage());
        }

        IdempotentRecord updated = idempotentRecordMapper.selectById(record.getId());
        return convertToIdempotentRecordVO(updated);
    }

    private IdempotentRecordVO doMarkFail(IdempotentRecord record, IdempotentHandleDTO dto) {
        LambdaUpdateWrapper<IdempotentRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IdempotentRecord::getId, record.getId())
                .set(IdempotentRecord::getProcessStatus, PROCESS_STATUS_FAILED)
                .set(IdempotentRecord::getOperatorType, OPERATOR_TYPE_MANUAL_MARK_FAIL)
                .set(IdempotentRecord::getOperator, dto.getOperator())
                .set(IdempotentRecord::getOperateTime, LocalDateTime.now())
                .set(IdempotentRecord::getRemark, dto.getRemark());
        idempotentRecordMapper.update(null, wrapper);

        IdempotentRecord updated = idempotentRecordMapper.selectById(record.getId());
        return convertToIdempotentRecordVO(updated);
    }

    @Override
    public IPage<IdempotentRecordVO> queryIdempotentRecords(Integer processStatus, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<IdempotentRecord> wrapper = new LambdaQueryWrapper<>();
        if (processStatus != null) {
            wrapper.eq(IdempotentRecord::getProcessStatus, processStatus);
        }
        wrapper.orderByDesc(IdempotentRecord::getOperateTime);

        Page<IdempotentRecord> page = new Page<>(pageNum, pageSize);
        IPage<IdempotentRecord> recordPage = idempotentRecordMapper.selectPage(page, wrapper);

        Page<IdempotentRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<IdempotentRecordVO> voList = recordPage.getRecords().stream()
                .map(this::convertToIdempotentRecordVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<BenefitChainItemVO> convertToChainItems(List<BenefitUseLog> logs) {
        List<BenefitChainItemVO> items = new ArrayList<>();
        int stepNo = 1;
        for (BenefitUseLog log : logs) {
            if (log.getLockTime() != null) {
                BenefitChainItemVO item = new BenefitChainItemVO();
                item.setStepNo(stepNo++);
                item.setStepName("权益锁定");
                item.setUseNo(log.getUseNo());
                item.setBenefitType(log.getBenefitType());
                item.setBenefitTypeName(getBenefitTypeName(log.getBenefitType()));
                item.setBenefitValue(log.getBenefitValue());
                item.setUsedPoints(log.getUsedPoints());
                item.setUseStatus(USE_STATUS_LOCKED);
                item.setUseStatusName("已锁定");
                item.setOperator(log.getOperator());
                item.setOperateTime(log.getLockTime());
                item.setStoreCode(log.getStoreCode());
                item.setPosCode(log.getPosCode());
                items.add(item);
            }
            if (log.getConfirmTime() != null) {
                BenefitChainItemVO item = new BenefitChainItemVO();
                item.setStepNo(stepNo++);
                item.setStepName("权益核销");
                item.setUseNo(log.getUseNo());
                item.setBenefitType(log.getBenefitType());
                item.setBenefitTypeName(getBenefitTypeName(log.getBenefitType()));
                item.setBenefitValue(log.getBenefitValue());
                item.setUsedPoints(log.getUsedPoints());
                item.setUseStatus(USE_STATUS_CONFIRMED);
                item.setUseStatusName("核销成功");
                item.setOperator(log.getOperator());
                item.setOperateTime(log.getConfirmTime());
                item.setStoreCode(log.getStoreCode());
                item.setPosCode(log.getPosCode());
                items.add(item);
            }
            if (log.getReturnTime() != null) {
                BenefitChainItemVO item = new BenefitChainItemVO();
                item.setStepNo(stepNo++);
                item.setStepName("权益返还");
                item.setUseNo(log.getUseNo());
                item.setBenefitType(log.getBenefitType());
                item.setBenefitTypeName(getBenefitTypeName(log.getBenefitType()));
                item.setBenefitValue(log.getBenefitValue());
                item.setUsedPoints(log.getUsedPoints());
                item.setUseStatus(USE_STATUS_RETURNED);
                item.setUseStatusName("已返还");
                item.setOperator(log.getOperator());
                item.setOperateTime(log.getReturnTime());
                item.setStoreCode(log.getStoreCode());
                item.setPosCode(log.getPosCode());
                items.add(item);
            }
        }
        return items;
    }

    private IdempotentRecordVO convertToIdempotentRecordVO(IdempotentRecord record) {
        IdempotentRecordVO vo = new IdempotentRecordVO();
        vo.setId(record.getId());
        vo.setBusinessNo(record.getBusinessNo());
        vo.setBusinessType(record.getBusinessType());
        vo.setBusinessTypeName(getBusinessTypeName(record.getBusinessType()));
        vo.setProcessStatus(record.getProcessStatus());
        vo.setProcessStatusName(getProcessStatusName(record.getProcessStatus()));
        vo.setRequestId(record.getRequestId());
        vo.setRetryCount(record.getRetryCount());
        vo.setOperator(record.getOperator());
        vo.setOperatorType(record.getOperatorType());
        vo.setOperatorTypeName(getOperatorTypeName(record.getOperatorType()));
        vo.setOperateTime(record.getOperateTime());
        vo.setRemark(record.getRemark());
        vo.setCanReplay(record.getProcessStatus() == PROCESS_STATUS_PROCESSING
                || record.getProcessStatus() == PROCESS_STATUS_FAILED);
        vo.setCanMarkFail(record.getProcessStatus() == PROCESS_STATUS_PROCESSING);
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
            return "";
        }
        switch (type) {
            case 1: return "优惠券";
            case 2: return "积分抵扣";
            case 3: return "等级折扣";
            case 4: return "兑换权益";
            default: return "未知";
        }
    }

    private String getBusinessTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case BIZ_TYPE_PAY_LOCK: return "支付锁定";
            case BIZ_TYPE_CONFIRM: return "完成核销";
            case BIZ_TYPE_REFUND_RETURN: return "退款返还";
            default: return "未知";
        }
    }

    private String getProcessStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case PROCESS_STATUS_PROCESSING: return "处理中";
            case PROCESS_STATUS_COMPLETED: return "已完成";
            case PROCESS_STATUS_FAILED: return "已失败";
            default: return "未知";
        }
    }

    private String getOperatorTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case OPERATOR_TYPE_SYSTEM: return "系统";
            case OPERATOR_TYPE_MANUAL_REPLAY: return "人工重放";
            case OPERATOR_TYPE_MANUAL_MARK_FAIL: return "人工标记失败";
            default: return "未知";
        }
    }
}
