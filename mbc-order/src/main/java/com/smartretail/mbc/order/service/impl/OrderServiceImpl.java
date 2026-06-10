package com.smartretail.mbc.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.dto.BenefitReturnDTO;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.service.BenefitService;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.enums.OrderStatusEnum;
import com.smartretail.mbc.common.enums.PointSourceEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.level.dto.GrowthCalcDTO;
import com.smartretail.mbc.level.entity.LevelRule;
import com.smartretail.mbc.level.mapper.LevelRuleMapper;
import com.smartretail.mbc.level.service.LevelService;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import com.smartretail.mbc.order.dto.OrderCompleteDTO;
import com.smartretail.mbc.order.dto.OrderCreateDTO;
import com.smartretail.mbc.order.dto.OrderPayDTO;
import com.smartretail.mbc.order.dto.OrderQueryDTO;
import com.smartretail.mbc.order.dto.OrderRefundDTO;
import com.smartretail.mbc.order.dto.OrderValidateDTO;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.order.service.OrderService;
import com.smartretail.mbc.order.vo.OrderStatisticsVO;
import com.smartretail.mbc.order.vo.OrderValidateResultVO;
import com.smartretail.mbc.order.vo.OrderVO;
import com.smartretail.mbc.point.dto.PointAddDTO;
import com.smartretail.mbc.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ConsumeOrderMapper consumeOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberMapper memberMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final LevelRuleMapper levelRuleMapper;
    private final BenefitService benefitService;
    private final PointService pointService;
    private final LevelService levelService;

    private static final String ORDER_CREATE_KEY = "mbc:order:create:";
    private static final int IDEMPOTENT_EXPIRE_MINUTES = 30;
    private static final BigDecimal POINT_RATIO = new BigDecimal("100");
    private static final BigDecimal MAX_POINT_RATIO = new BigDecimal("0.5");

    @Override
    public OrderValidateResultVO validateOrder(OrderValidateDTO dto) {
        OrderValidateResultVO result = new OrderValidateResultVO();
        List<String> invalidReasons = new ArrayList<>();
        List<OrderValidateResultVO.AppliedCouponVO> appliedCoupons = new ArrayList<>();
        BigDecimal couponAmount = BigDecimal.ZERO;
        BigDecimal pointAmount = BigDecimal.ZERO;
        BigDecimal levelDiscount = BigDecimal.ZERO;
        Integer earnablePoints = 0;
        Integer earnableGrowth = 0;

        BigDecimal totalAmount = dto.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            invalidReasons.add("订单金额必须大于0");
            result.setValid(false);
            result.setInvalidReasons(invalidReasons);
            return result;
        }

        Member member = null;
        LevelRule levelRule = null;
        if (dto.getMemberId() != null) {
            member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                invalidReasons.add("会员不存在");
            } else if (member.getStatus() != null && member.getStatus() != 1) {
                invalidReasons.add("会员状态异常");
            } else {
                levelRule = levelRuleMapper.selectOne(
                        new LambdaQueryWrapper<LevelRule>()
                                .eq(LevelRule::getLevelCode, member.getLevelCode())
                                .eq(LevelRule::getStatus, 1)
                );
                if (levelRule == null) {
                    levelRule = getDefaultLevelRule();
                }
            }
        }

        List<Long> usedCouponIds = dto.getUsedCouponIds();
        if (usedCouponIds != null && !usedCouponIds.isEmpty()) {
            if (member == null) {
                invalidReasons.add("非会员不可使用优惠券");
            } else {
                boolean hasNonStackable = false;
                LocalDateTime now = LocalDateTime.now();
                for (Long couponId : usedCouponIds) {
                    CouponInstance instance = couponInstanceMapper.selectById(couponId);
                    if (instance == null) {
                        invalidReasons.add("优惠券不存在: " + couponId);
                        continue;
                    }
                    if (!instance.getMemberId().equals(member.getId())) {
                        invalidReasons.add("优惠券不属于该会员: " + couponId);
                        continue;
                    }
                    if (!CouponStatusEnum.AVAILABLE.getCode().equals(instance.getCouponStatus())) {
                        invalidReasons.add("优惠券状态不可用: " + couponId);
                        continue;
                    }
                    if (instance.getValidStart() != null && now.isBefore(instance.getValidStart())) {
                        invalidReasons.add("优惠券尚未生效: " + couponId);
                        continue;
                    }
                    if (instance.getValidEnd() != null && now.isAfter(instance.getValidEnd())) {
                        invalidReasons.add("优惠券已过期: " + couponId);
                        continue;
                    }
                    CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
                    if (template == null) {
                        invalidReasons.add("优惠券模板不存在: " + couponId);
                        continue;
                    }
                    if (template.getFullAmount() != null && totalAmount.compareTo(template.getFullAmount()) < 0) {
                        invalidReasons.add("优惠券不满足使用门槛: " + couponId);
                        continue;
                    }
                    Integer stackable = template.getStackable();
                    if (stackable != null && stackable == 0) {
                        if (hasNonStackable) {
                            invalidReasons.add("不可叠加的优惠券只能使用一张");
                            continue;
                        }
                        hasNonStackable = true;
                    }
                    BigDecimal reduceAmount = template.getReduceAmount() != null ? template.getReduceAmount() : BigDecimal.ZERO;
                    couponAmount = couponAmount.add(reduceAmount);

                    OrderValidateResultVO.AppliedCouponVO couponVO = new OrderValidateResultVO.AppliedCouponVO();
                    couponVO.setCouponInstanceId(couponId);
                    couponVO.setTemplateId(template.getId());
                    couponVO.setCouponName(template.getCouponName());
                    couponVO.setCouponType(template.getCouponType());
                    couponVO.setFullAmount(template.getFullAmount());
                    couponVO.setReduceAmount(reduceAmount);
                    couponVO.setStackable(stackable);
                    appliedCoupons.add(couponVO);
                }
            }
        }

        Integer usedPoints = dto.getUsedPoints() != null ? dto.getUsedPoints() : 0;
        if (usedPoints > 0) {
            if (member == null) {
                invalidReasons.add("非会员不可使用积分");
            } else {
                Integer currentPoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
                if (currentPoints < usedPoints) {
                    invalidReasons.add("可用积分不足");
                }
                BigDecimal maxPointAmount = totalAmount.multiply(MAX_POINT_RATIO);
                BigDecimal usedPointAmount = new BigDecimal(usedPoints).divide(POINT_RATIO, 2, RoundingMode.HALF_UP);
                if (usedPointAmount.compareTo(maxPointAmount) > 0) {
                    invalidReasons.add("积分抵扣最多抵订单金额的50%");
                }
                if (invalidReasons.isEmpty() || invalidReasons.stream().noneMatch(r -> r.contains("积分"))) {
                    pointAmount = usedPointAmount;
                }
            }
        }

        if (member != null && levelRule != null && invalidReasons.stream().noneMatch(r -> r.contains("会员"))) {
            BigDecimal discountRate = levelRule.getDiscountRate() != null ? levelRule.getDiscountRate() : BigDecimal.ZERO;
            if (discountRate.compareTo(BigDecimal.ZERO) > 0 && discountRate.compareTo(BigDecimal.TEN) <= 0) {
                levelDiscount = totalAmount.multiply(BigDecimal.TEN.subtract(discountRate)).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalDiscount = couponAmount.add(pointAmount).add(levelDiscount);
        BigDecimal finalPayAmount = totalAmount.subtract(totalDiscount);
        if (finalPayAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalPayAmount = BigDecimal.ZERO;
        }

        if (member != null && levelRule != null && invalidReasons.stream().noneMatch(r -> r.contains("会员"))) {
            BigDecimal pointRatio = levelRule.getPointRatio() != null ? levelRule.getPointRatio() : BigDecimal.ZERO;
            BigDecimal growthRatio = levelRule.getGrowthRatio() != null ? levelRule.getGrowthRatio() : BigDecimal.ZERO;
            earnablePoints = finalPayAmount.multiply(pointRatio).setScale(0, RoundingMode.DOWN).intValue();
            earnableGrowth = finalPayAmount.multiply(growthRatio).setScale(0, RoundingMode.DOWN).intValue();
        }

        result.setValid(invalidReasons.isEmpty());
        result.setInvalidReasons(invalidReasons);
        result.setCouponAmount(couponAmount);
        result.setPointAmount(pointAmount);
        result.setLevelDiscount(levelDiscount);
        result.setTotalDiscount(totalDiscount);
        result.setFinalPayAmount(finalPayAmount);
        result.setEarnablePoints(earnablePoints);
        result.setEarnableGrowth(earnableGrowth);
        result.setAppliedCoupons(appliedCoupons);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        String redisKey = ORDER_CREATE_KEY + dto.getOrderNo();
        Boolean setResult = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(setResult)) {
            ConsumeOrder existOrder = consumeOrderMapper.selectByOrderNo(dto.getOrderNo());
            if (existOrder != null) {
                return convertToVO(existOrder);
            }
        }

        try {
            OrderValidateDTO validateDTO = new OrderValidateDTO();
            validateDTO.setMemberId(dto.getMemberId());
            validateDTO.setTotalAmount(dto.getTotalAmount());
            validateDTO.setUsedCouponIds(dto.getUsedCouponInstanceIds());
            validateDTO.setUsedPoints(dto.getUsedPoints());
            validateDTO.setStoreCode(dto.getStoreCode());
            OrderValidateResultVO validateResult = validateOrder(validateDTO);
            if (!validateResult.getValid()) {
                throw new BusinessException("订单校验失败: " + String.join("; ", validateResult.getInvalidReasons()));
            }

            ConsumeOrder order = new ConsumeOrder();
            BeanUtils.copyProperties(dto, order);
            order.setOrderStatus(OrderStatusEnum.CREATE.getCode());
            order.setCouponAmount(validateResult.getCouponAmount());
            order.setPointAmount(validateResult.getPointAmount());
            order.setLevelDiscount(validateResult.getLevelDiscount());
            order.setDiscountAmount(validateResult.getTotalDiscount());
            order.setPayAmount(validateResult.getFinalPayAmount());
            order.setEarnedPoints(validateResult.getEarnablePoints());
            order.setEarnedGrowth(validateResult.getEarnableGrowth());
            if (dto.getUsedCouponInstanceIds() != null && !dto.getUsedCouponInstanceIds().isEmpty()) {
                order.setUsedCouponIds(String.join(",", dto.getUsedCouponInstanceIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            consumeOrderMapper.insert(order);
            return convertToVO(order);
        } catch (Exception e) {
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(OrderPayDTO dto) {
        ConsumeOrder order = consumeOrderMapper.selectByOrderNo(dto.getOrderNo());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatusEnum.CREATE.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态错误，当前状态: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setPayAmount(dto.getPayAmount() != null ? dto.getPayAmount() : order.getPayAmount());
        order.setPayTime(dto.getPayTime() != null ? dto.getPayTime() : LocalDateTime.now());
        consumeOrderMapper.updateById(order);

        if (order.getMemberId() != null) {
            try {
                BenefitLockDTO lockDTO = new BenefitLockDTO();
                lockDTO.setOrderNo(order.getOrderNo());
                lockDTO.setMemberId(order.getMemberId());
                lockDTO.setOrderAmount(order.getTotalAmount());
                lockDTO.setStoreCode(order.getStoreCode());
                lockDTO.setPosCode(order.getPosCode());
                lockDTO.setOperator(order.getCashier());

                if (order.getUsedCouponIds() != null && !order.getUsedCouponIds().isEmpty()) {
                    List<Long> couponIds = parseCouponIds(order.getUsedCouponIds());
                    lockDTO.setBenefitType(1);
                    lockDTO.setBenefitId(couponIds);
                    benefitService.lockBenefits(lockDTO);
                }

                if (order.getUsedPoints() != null && order.getUsedPoints() > 0) {
                    lockDTO.setBenefitType(2);
                    lockDTO.setBenefitId(null);
                    lockDTO.setUsedPoints(order.getUsedPoints());
                    benefitService.lockBenefits(lockDTO);
                }

                if (order.getLevelDiscount() != null && order.getLevelDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    lockDTO.setBenefitType(3);
                    lockDTO.setBenefitId(null);
                    lockDTO.setUsedPoints(null);
                    benefitService.lockBenefits(lockDTO);
                }
            } catch (Exception e) {
                log.error("锁定权益失败 orderNo: {}", order.getOrderNo(), e);
            }
        }

        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO completeOrder(OrderCompleteDTO dto) {
        ConsumeOrder order = consumeOrderMapper.selectByOrderNo(dto.getOrderNo());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态错误，当前状态: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setCompleteTime(LocalDateTime.now());
        consumeOrderMapper.updateById(order);

        if (order.getMemberId() != null) {
            BenefitConfirmDTO confirmDTO = new BenefitConfirmDTO();
            confirmDTO.setOrderNo(order.getOrderNo());
            confirmDTO.setMemberId(order.getMemberId());
            confirmDTO.setOrderAmount(order.getPayAmount());
            confirmDTO.setStoreCode(order.getStoreCode());
            confirmDTO.setPosCode(order.getPosCode());
            confirmDTO.setOperator(order.getCashier());
            try {
                benefitService.confirmBenefits(confirmDTO);
            } catch (Exception e) {
                log.error("确认权益失败 orderNo: {}", order.getOrderNo(), e);
            }

            if (order.getEarnedPoints() != null && order.getEarnedPoints() > 0) {
                PointAddDTO pointAddDTO = new PointAddDTO();
                pointAddDTO.setMemberId(order.getMemberId());
                pointAddDTO.setPoints(order.getEarnedPoints());
                pointAddDTO.setSourceType(PointSourceEnum.CONSUME.getCode());
                pointAddDTO.setSourceId(order.getOrderNo());
                pointAddDTO.setRemark("订单消费获赠积分");
                try {
                    pointService.addPoints(pointAddDTO);
                } catch (Exception e) {
                    log.error("发放积分失败 orderNo: {}", order.getOrderNo(), e);
                }
            }

            if (order.getEarnedGrowth() != null && order.getEarnedGrowth() > 0) {
                GrowthCalcDTO growthCalcDTO = new GrowthCalcDTO();
                growthCalcDTO.setMemberId(order.getMemberId());
                growthCalcDTO.setOrderAmount(order.getPayAmount());
                growthCalcDTO.setSourceType(1);
                growthCalcDTO.setSourceId(order.getOrderNo());
                try {
                    levelService.calcAndAddGrowth(growthCalcDTO);
                } catch (Exception e) {
                    log.error("增加成长值失败 orderNo: {}", order.getOrderNo(), e);
                }
            }

            updateCouponUsedCount(order);
        }

        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO refundOrder(OrderRefundDTO dto) {
        ConsumeOrder order = consumeOrderMapper.selectByOrderNo(dto.getOrderNo());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer status = order.getOrderStatus();
        if (!OrderStatusEnum.PAID.getCode().equals(status) && !OrderStatusEnum.COMPLETED.getCode().equals(status)) {
            throw new BusinessException("订单状态错误，当前状态: " + status);
        }

        order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        order.setRefundTime(LocalDateTime.now());
        order.setRefundNo(dto.getRefundNo());
        order.setRefundAmount(dto.getRefundAmount());
        if (dto.getReason() != null) {
            order.setRemark(dto.getReason());
        }
        consumeOrderMapper.updateById(order);

        if (order.getMemberId() != null) {
            BenefitReturnDTO returnDTO = new BenefitReturnDTO();
            returnDTO.setOrderNo(order.getOrderNo());
            returnDTO.setRefundNo(dto.getRefundNo());
            returnDTO.setMemberId(order.getMemberId());
            returnDTO.setReturnReason(dto.getReason());
            try {
                benefitService.returnBenefits(returnDTO);
            } catch (Exception e) {
                log.error("返还权益失败 orderNo: {}", order.getOrderNo(), e);
            }
        }

        return convertToVO(order);
    }

    @Override
    public IPage<OrderVO> pageOrders(OrderQueryDTO dto) {
        Page<ConsumeOrder> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
        if (dto.getMemberId() != null) {
            wrapper.eq(ConsumeOrder::getMemberId, dto.getMemberId());
        }
        if (dto.getOrderStatus() != null) {
            wrapper.eq(ConsumeOrder::getOrderStatus, dto.getOrderStatus());
        }
        if (dto.getOrderType() != null) {
            wrapper.eq(ConsumeOrder::getOrderType, dto.getOrderType());
        }
        if (dto.getStoreCode() != null && !dto.getStoreCode().isEmpty()) {
            wrapper.eq(ConsumeOrder::getStoreCode, dto.getStoreCode());
        }
        if (dto.getChannel() != null && !dto.getChannel().isEmpty()) {
            wrapper.eq(ConsumeOrder::getChannel, dto.getChannel());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(ConsumeOrder::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(ConsumeOrder::getCreateTime, dto.getEndTime());
        }
        wrapper.orderByDesc(ConsumeOrder::getCreateTime);

        IPage<ConsumeOrder> orderPage = consumeOrderMapper.selectPage(page, wrapper);
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public OrderStatisticsVO getStatistics(OrderQueryDTO dto) {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
        if (dto.getStoreCode() != null && !dto.getStoreCode().isEmpty()) {
            wrapper.eq(ConsumeOrder::getStoreCode, dto.getStoreCode());
        }
        if (dto.getChannel() != null && !dto.getChannel().isEmpty()) {
            wrapper.eq(ConsumeOrder::getChannel, dto.getChannel());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(ConsumeOrder::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(ConsumeOrder::getCreateTime, dto.getEndTime());
        }
        wrapper.ne(ConsumeOrder::getOrderStatus, OrderStatusEnum.CANCELLED.getCode());

        List<ConsumeOrder> allOrders = consumeOrderMapper.selectList(wrapper);
        vo.setTotalOrders((long) allOrders.size());
        vo.setTotalPayAmount(allOrders.stream()
                .filter(o -> o.getPayAmount() != null)
                .map(ConsumeOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setTotalDiscount(allOrders.stream()
                .filter(o -> o.getDiscountAmount() != null)
                .map(ConsumeOrder::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<ConsumeOrder> memberOrders = allOrders.stream()
                .filter(o -> o.getMemberId() != null)
                .collect(Collectors.toList());
        vo.setMemberOrders((long) memberOrders.size());

        BigDecimal totalPay = vo.getTotalPayAmount();
        if (totalPay != null && totalPay.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal memberPay = memberOrders.stream()
                    .filter(o -> o.getPayAmount() != null)
                    .map(ConsumeOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setMemberPayRatio(memberPay.divide(totalPay, 4, RoundingMode.HALF_UP));
        } else {
            vo.setMemberPayRatio(BigDecimal.ZERO);
        }
        return vo;
    }

    private OrderVO convertToVO(ConsumeOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            if (statusEnum.getCode().equals(order.getOrderStatus())) {
                vo.setOrderStatusName(statusEnum.getName());
                break;
            }
        }
        if (order.getMemberId() != null) {
            Member member = memberMapper.selectById(order.getMemberId());
            if (member != null) {
                MemberSimpleVO simpleVO = new MemberSimpleVO();
                BeanUtils.copyProperties(member, simpleVO);
                MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
                simpleVO.setLevelName(levelEnum.getName());
                vo.setMemberInfo(simpleVO);
            }
        }
        return vo;
    }

    private List<Long> parseCouponIds(String couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String id : couponIds.split(",")) {
            if (!id.trim().isEmpty()) {
                result.add(Long.parseLong(id.trim()));
            }
        }
        return result;
    }

    private void updateCouponUsedCount(ConsumeOrder order) {
        List<Long> couponIds = parseCouponIds(order.getUsedCouponIds());
        for (Long couponId : couponIds) {
            CouponInstance instance = couponInstanceMapper.selectById(couponId);
            if (instance != null && instance.getTemplateId() != null) {
                CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
                if (template != null) {
                    template.setUsedCount(template.getUsedCount() != null ? template.getUsedCount() + 1 : 1);
                    couponTemplateMapper.updateById(template);
                }
            }
        }
    }

    private LevelRule getDefaultLevelRule() {
        LevelRule rule = new LevelRule();
        rule.setLevelCode(1);
        rule.setLevelName("青铜");
        rule.setDiscountRate(BigDecimal.ZERO);
        rule.setPointRatio(BigDecimal.ONE);
        rule.setGrowthRatio(BigDecimal.ONE);
        return rule;
    }
}
