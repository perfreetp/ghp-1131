package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.entity.IdempotentRecord;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.mapper.IdempotentRecordMapper;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.PointTypeEnum;
import com.smartretail.mbc.common.enums.TimelineEventTypeEnum;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.level.entity.GrowthLog;
import com.smartretail.mbc.level.mapper.GrowthLogMapper;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.entity.MemberMergeLog;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.member.mapper.MemberMergeLogMapper;
import com.smartretail.mbc.message.entity.MessageLog;
import com.smartretail.mbc.message.mapper.MessageLogMapper;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.point.entity.PointLog;
import com.smartretail.mbc.point.mapper.PointLogMapper;
import com.smartretail.mbc.query.dto.MemberTimelineQueryDTO;
import com.smartretail.mbc.query.service.MemberTimelineService;
import com.smartretail.mbc.query.vo.MemberTimelineVO;
import com.smartretail.mbc.query.vo.TimelineEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTimelineServiceImpl implements MemberTimelineService {

    private final MemberMapper memberMapper;
    private final GrowthLogMapper growthLogMapper;
    private final PointLogMapper pointLogMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;
    private final MemberMergeLogMapper memberMergeLogMapper;
    private final MessageLogMapper messageLogMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;

    private static final int BIZ_TYPE_ORDER = 1;
    private static final int BIZ_TYPE_COUPON = 2;
    private static final int BIZ_TYPE_POINT = 3;
    private static final int BIZ_TYPE_LEVEL = 4;
    private static final int BIZ_TYPE_MERGE = 5;
    private static final int BIZ_TYPE_MESSAGE = 6;

    private static final int DIRECTION_ADD = 1;
    private static final int DIRECTION_SUBTRACT = -1;
    private static final int DIRECTION_NEUTRAL = 0;

    @Override
    public MemberTimelineVO getMemberTimeline(MemberTimelineQueryDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            MemberTimelineVO vo = new MemberTimelineVO();
            vo.setMemberId(dto.getMemberId());
            vo.setTotalCount(0L);
            vo.setEvents(new Page<>(dto.getPageNum(), dto.getPageSize()));
            return vo;
        }

        List<Integer> eventTypes = dto.getEventTypes();
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        int limit = dto.getPageSize() * 2;

        List<TimelineEventVO> allEvents = new ArrayList<>();

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.REGISTER)) {
            List<TimelineEventVO> registerEvents = getRegisterEvents(member, startTime, endTime);
            allEvents.addAll(registerEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.LEVEL_UP)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.LEVEL_DOWN)) {
            List<TimelineEventVO> levelEvents = getLevelEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(levelEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.POINT_ADD)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.POINT_SUBTRACT)) {
            List<TimelineEventVO> pointEvents = getPointEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(pointEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_RECEIVE)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_LOCK)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_USE)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_EXPIRE)) {
            List<TimelineEventVO> couponEvents = getCouponEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(couponEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.ORDER_PAY)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.ORDER_REFUND)) {
            List<TimelineEventVO> orderEvents = getOrderEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(orderEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_LOCK)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_USE)) {
            List<TimelineEventVO> benefitEvents = getBenefitEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(benefitEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.MEMBER_MERGE)) {
            List<TimelineEventVO> mergeEvents = getMergeEvents(dto.getMemberId(), startTime, endTime, limit);
            allEvents.addAll(mergeEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.MESSAGE_PUSH)) {
            List<TimelineEventVO> messageEvents = getMessageEvents(dto.getMemberId(), startTime, endTime, limit);
            allEvents.addAll(messageEvents);
        }

        if (isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.MANUAL_REPLAY)
                || isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.MANUAL_MARK_FAIL)) {
            List<TimelineEventVO> idempotentEvents = getIdempotentEvents(dto.getMemberId(), eventTypes, startTime, endTime, limit);
            allEvents.addAll(idempotentEvents);
        }

        allEvents.sort(Comparator.comparing(TimelineEventVO::getEventTime).reversed());

        long totalCount = allEvents.size();

        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allEvents.size());

        List<TimelineEventVO> pageRecords;
        if (fromIndex >= allEvents.size()) {
            pageRecords = new ArrayList<>();
        } else {
            pageRecords = allEvents.subList(fromIndex, toIndex);
        }

        Page<TimelineEventVO> page = new Page<>(pageNum, pageSize, totalCount);
        page.setRecords(pageRecords);

        MemberTimelineVO vo = new MemberTimelineVO();
        vo.setMemberId(member.getId());
        vo.setMemberName(member.getName());
        vo.setTotalCount(totalCount);
        vo.setEvents(page);

        return vo;
    }

    private boolean isEventTypeIncluded(List<Integer> eventTypes, TimelineEventTypeEnum eventType) {
        if (CollectionUtils.isEmpty(eventTypes)) {
            return true;
        }
        return eventTypes.contains(eventType.getCode());
    }

    private List<TimelineEventVO> getRegisterEvents(Member member, LocalDateTime startTime, LocalDateTime endTime) {
        List<TimelineEventVO> events = new ArrayList<>();
        LocalDateTime createTime = member.getCreateTime();
        if (createTime == null) {
            return events;
        }
        if (startTime != null && createTime.isBefore(startTime)) {
            return events;
        }
        if (endTime != null && createTime.isAfter(endTime)) {
            return events;
        }
        events.add(convertMemberRegister(member));
        return events;
    }

    private TimelineEventVO convertMemberRegister(Member member) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.REGISTER, member.getId()));
        vo.setEventType(TimelineEventTypeEnum.REGISTER.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.REGISTER.getName());
        vo.setEventTag(TimelineEventTypeEnum.REGISTER.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.REGISTER.getDesc());
        vo.setEventTime(member.getCreateTime());
        vo.setBizType(BIZ_TYPE_LEVEL);
        vo.setBizId(String.valueOf(member.getId()));
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(member.getRegisterSource());

        Map<String, Object> detail = new HashMap<>();
        detail.put("memberCode", member.getMemberCode());
        detail.put("registerSource", member.getRegisterSource());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getLevelEvents(Long memberId, List<Integer> eventTypes,
                                                  LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<GrowthLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthLog::getMemberId, memberId);
        wrapper.ne(GrowthLog::getBeforeLevel, GrowthLog::getAfterLevel);
        if (startTime != null) {
            wrapper.ge(GrowthLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(GrowthLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(GrowthLog::getCreateTime);
        wrapper.last("LIMIT " + limit);

        List<GrowthLog> logs = growthLogMapper.selectList(wrapper);

        return logs.stream()
                .map(this::convertGrowthLog)
                .filter(vo -> {
                    if (CollectionUtils.isEmpty(eventTypes)) {
                        return true;
                    }
                    return eventTypes.contains(vo.getEventType());
                })
                .collect(Collectors.toList());
    }

    private TimelineEventVO convertGrowthLog(GrowthLog log) {
        TimelineEventVO vo = new TimelineEventVO();
        boolean isLevelUp = log.getAfterLevel() > log.getBeforeLevel();
        TimelineEventTypeEnum eventType = isLevelUp
                ? TimelineEventTypeEnum.LEVEL_UP
                : TimelineEventTypeEnum.LEVEL_DOWN;

        vo.setEventId(generateEventId(eventType, log.getId()));
        vo.setEventType(eventType.getCode());
        vo.setEventTypeName(eventType.getName());
        vo.setEventTag(eventType.getTag());
        vo.setEventDesc(eventType.getDesc());
        vo.setEventTime(log.getCreateTime());
        vo.setBizType(BIZ_TYPE_LEVEL);
        vo.setBizId(String.valueOf(log.getId()));
        vo.setDirection(isLevelUp ? DIRECTION_ADD : DIRECTION_SUBTRACT);
        vo.setRelatedStaff(log.getCreateBy());

        Map<String, Object> detail = new HashMap<>();
        detail.put("beforeLevel", log.getBeforeLevel());
        detail.put("afterLevel", log.getAfterLevel());
        detail.put("changeValue", log.getChangeValue());
        detail.put("sourceType", log.getSourceType());
        detail.put("sourceId", log.getSourceId());
        detail.put("remark", log.getRemark());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getPointEvents(Long memberId, List<Integer> eventTypes,
                                                  LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointLog::getMemberId, memberId);
        if (startTime != null) {
            wrapper.ge(PointLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(PointLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(PointLog::getCreateTime);
        wrapper.last("LIMIT " + limit);

        List<PointLog> logs = pointLogMapper.selectList(wrapper);

        return logs.stream()
                .map(this::convertPointLog)
                .filter(vo -> {
                    if (CollectionUtils.isEmpty(eventTypes)) {
                        return true;
                    }
                    return eventTypes.contains(vo.getEventType());
                })
                .collect(Collectors.toList());
    }

    private TimelineEventVO convertPointLog(PointLog log) {
        TimelineEventVO vo = new TimelineEventVO();
        Integer pointType = log.getPointType();
        boolean isAdd = Objects.equals(pointType, PointTypeEnum.ADD.getCode())
                || Objects.equals(pointType, PointTypeEnum.UNFREEZE.getCode());

        TimelineEventTypeEnum eventType = isAdd
                ? TimelineEventTypeEnum.POINT_ADD
                : TimelineEventTypeEnum.POINT_SUBTRACT;

        vo.setEventId(generateEventId(eventType, log.getId()));
        vo.setEventType(eventType.getCode());
        vo.setEventTypeName(eventType.getName());
        vo.setEventTag(eventType.getTag());
        vo.setEventDesc(eventType.getDesc());
        vo.setEventTime(log.getCreateTime());
        vo.setBizType(BIZ_TYPE_POINT);
        vo.setBizId(String.valueOf(log.getId()));
        vo.setPoints(Math.abs(log.getChangePoints()));
        vo.setDirection(isAdd ? DIRECTION_ADD : DIRECTION_SUBTRACT);

        Map<String, Object> detail = new HashMap<>();
        detail.put("pointType", log.getPointType());
        detail.put("changePoints", log.getChangePoints());
        detail.put("beforePoints", log.getBeforePoints());
        detail.put("afterPoints", log.getAfterPoints());
        detail.put("frozenPoints", log.getFrozenPoints());
        detail.put("sourceType", log.getSourceType());
        detail.put("sourceId", log.getSourceId());
        detail.put("remark", log.getRemark());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getCouponEvents(Long memberId, List<Integer> eventTypes,
                                                   LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<CouponInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponInstance::getMemberId, memberId);
        wrapper.orderByDesc(CouponInstance::getCreateTime);
        wrapper.last("LIMIT " + limit);

        List<CouponInstance> instances = couponInstanceMapper.selectList(wrapper);

        List<TimelineEventVO> events = new ArrayList<>();
        for (CouponInstance instance : instances) {
            List<TimelineEventVO> instanceEvents = convertCouponInstance(instance);
            for (TimelineEventVO event : instanceEvents) {
                if (startTime != null && event.getEventTime().isBefore(startTime)) {
                    continue;
                }
                if (endTime != null && event.getEventTime().isAfter(endTime)) {
                    continue;
                }
                if (CollectionUtils.isEmpty(eventTypes) || eventTypes.contains(event.getEventType())) {
                    events.add(event);
                }
            }
        }
        return events;
    }

    private List<TimelineEventVO> convertCouponInstance(CouponInstance instance) {
        List<TimelineEventVO> events = new ArrayList<>();

        if (instance.getReceiveTime() != null) {
            TimelineEventVO receiveEvent = new TimelineEventVO();
            receiveEvent.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_RECEIVE, instance.getInstanceNo()));
            receiveEvent.setEventType(TimelineEventTypeEnum.COUPON_RECEIVE.getCode());
            receiveEvent.setEventTypeName(TimelineEventTypeEnum.COUPON_RECEIVE.getName());
            receiveEvent.setEventTag(TimelineEventTypeEnum.COUPON_RECEIVE.getTag());
            receiveEvent.setEventDesc(TimelineEventTypeEnum.COUPON_RECEIVE.getDesc());
            receiveEvent.setEventTime(instance.getReceiveTime());
            receiveEvent.setBizType(BIZ_TYPE_COUPON);
            receiveEvent.setBizId(instance.getInstanceNo());
            receiveEvent.setDirection(DIRECTION_ADD);
            receiveEvent.setRelatedStaff(instance.getReceiveSource());

            Map<String, Object> detail = new HashMap<>();
            detail.put("templateId", instance.getTemplateId());
            detail.put("receiveSource", instance.getReceiveSource());
            detail.put("validStart", instance.getValidStart());
            detail.put("validEnd", instance.getValidEnd());
            detail.put("remark", instance.getRemark());
            receiveEvent.setDetail(detail);

            events.add(receiveEvent);
        }

        if (instance.getLockedTime() != null) {
            TimelineEventVO lockEvent = new TimelineEventVO();
            lockEvent.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_LOCK, instance.getInstanceNo() + "_LOCK"));
            lockEvent.setEventType(TimelineEventTypeEnum.COUPON_LOCK.getCode());
            lockEvent.setEventTypeName(TimelineEventTypeEnum.COUPON_LOCK.getName());
            lockEvent.setEventTag(TimelineEventTypeEnum.COUPON_LOCK.getTag());
            lockEvent.setEventDesc(TimelineEventTypeEnum.COUPON_LOCK.getDesc());
            lockEvent.setEventTime(instance.getLockedTime());
            lockEvent.setBizType(BIZ_TYPE_COUPON);
            lockEvent.setBizId(instance.getLockOrderNo());
            lockEvent.setDirection(DIRECTION_NEUTRAL);

            Map<String, Object> detail = new HashMap<>();
            detail.put("templateId", instance.getTemplateId());
            detail.put("lockOrderNo", instance.getLockOrderNo());
            lockEvent.setDetail(detail);

            events.add(lockEvent);
        }

        if (instance.getUsedTime() != null) {
            TimelineEventVO useEvent = new TimelineEventVO();
            useEvent.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_USE, instance.getInstanceNo() + "_USE"));
            useEvent.setEventType(TimelineEventTypeEnum.COUPON_USE.getCode());
            useEvent.setEventTypeName(TimelineEventTypeEnum.COUPON_USE.getName());
            useEvent.setEventTag(TimelineEventTypeEnum.COUPON_USE.getTag());
            useEvent.setEventDesc(TimelineEventTypeEnum.COUPON_USE.getDesc());
            useEvent.setEventTime(instance.getUsedTime());
            useEvent.setBizType(BIZ_TYPE_COUPON);
            useEvent.setBizId(instance.getUsedOrderNo());
            useEvent.setDirection(DIRECTION_SUBTRACT);

            Map<String, Object> detail = new HashMap<>();
            detail.put("templateId", instance.getTemplateId());
            detail.put("usedOrderNo", instance.getUsedOrderNo());
            useEvent.setDetail(detail);

            events.add(useEvent);
        }

        if (Objects.equals(instance.getCouponStatus(), CouponStatusEnum.EXPIRED.getCode())) {
            LocalDateTime expireTime = instance.getValidEnd();
            if (expireTime != null) {
                TimelineEventVO expireEvent = new TimelineEventVO();
                expireEvent.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_EXPIRE, instance.getInstanceNo() + "_EXP"));
                expireEvent.setEventType(TimelineEventTypeEnum.COUPON_EXPIRE.getCode());
                expireEvent.setEventTypeName(TimelineEventTypeEnum.COUPON_EXPIRE.getName());
                expireEvent.setEventTag(TimelineEventTypeEnum.COUPON_EXPIRE.getTag());
                expireEvent.setEventDesc(TimelineEventTypeEnum.COUPON_EXPIRE.getDesc());
                expireEvent.setEventTime(expireTime);
                expireEvent.setBizType(BIZ_TYPE_COUPON);
                expireEvent.setBizId(instance.getInstanceNo());
                expireEvent.setDirection(DIRECTION_SUBTRACT);

                Map<String, Object> detail = new HashMap<>();
                detail.put("templateId", instance.getTemplateId());
                detail.put("validEnd", instance.getValidEnd());
                expireEvent.setDetail(detail);

                events.add(expireEvent);
            }
        }

        return events;
    }

    private List<TimelineEventVO> getOrderEvents(Long memberId, List<Integer> eventTypes,
                                                  LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumeOrder::getMemberId, memberId);
        if (startTime != null) {
            wrapper.and(w -> w.ge(ConsumeOrder::getPayTime, startTime)
                    .or().ge(ConsumeOrder::getRefundTime, startTime));
        }
        if (endTime != null) {
            wrapper.and(w -> w.le(ConsumeOrder::getPayTime, endTime)
                    .or().le(ConsumeOrder::getRefundTime, endTime));
        }
        wrapper.orderByDesc(ConsumeOrder::getPayTime);
        wrapper.last("LIMIT " + limit);

        List<ConsumeOrder> orders = consumeOrderMapper.selectList(wrapper);

        List<TimelineEventVO> events = new ArrayList<>();
        for (ConsumeOrder order : orders) {
            if (order.getPayTime() != null
                    && isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.ORDER_PAY)
                    && (startTime == null || !order.getPayTime().isBefore(startTime))
                    && (endTime == null || !order.getPayTime().isAfter(endTime))) {
                events.add(convertOrderPay(order));
            }
            if (order.getRefundTime() != null
                    && isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.ORDER_REFUND)
                    && (startTime == null || !order.getRefundTime().isBefore(startTime))
                    && (endTime == null || !order.getRefundTime().isAfter(endTime))) {
                events.add(convertOrderRefund(order));
            }
        }
        return events;
    }

    private TimelineEventVO convertOrderPay(ConsumeOrder order) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.ORDER_PAY, order.getOrderNo()));
        vo.setEventType(TimelineEventTypeEnum.ORDER_PAY.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.ORDER_PAY.getName());
        vo.setEventTag(TimelineEventTypeEnum.ORDER_PAY.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.ORDER_PAY.getDesc());
        vo.setEventTime(order.getPayTime());
        vo.setBizType(BIZ_TYPE_ORDER);
        vo.setBizId(order.getOrderNo());
        vo.setAmount(order.getPayAmount());
        vo.setPoints(order.getEarnedPoints());
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(order.getCashier());

        Map<String, Object> detail = new HashMap<>();
        detail.put("orderNo", order.getOrderNo());
        detail.put("orderType", order.getOrderType());
        detail.put("totalAmount", order.getTotalAmount());
        detail.put("discountAmount", order.getDiscountAmount());
        detail.put("couponAmount", order.getCouponAmount());
        detail.put("pointAmount", order.getPointAmount());
        detail.put("levelDiscount", order.getLevelDiscount());
        detail.put("payAmount", order.getPayAmount());
        detail.put("earnedPoints", order.getEarnedPoints());
        detail.put("earnedGrowth", order.getEarnedGrowth());
        detail.put("storeName", order.getStoreName());
        detail.put("channel", order.getChannel());
        detail.put("remark", order.getRemark());
        vo.setDetail(detail);

        return vo;
    }

    private TimelineEventVO convertOrderRefund(ConsumeOrder order) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.ORDER_REFUND, order.getRefundNo()));
        vo.setEventType(TimelineEventTypeEnum.ORDER_REFUND.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.ORDER_REFUND.getName());
        vo.setEventTag(TimelineEventTypeEnum.ORDER_REFUND.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.ORDER_REFUND.getDesc());
        vo.setEventTime(order.getRefundTime());
        vo.setBizType(BIZ_TYPE_ORDER);
        vo.setBizId(order.getRefundNo());
        vo.setAmount(order.getRefundAmount());
        vo.setDirection(DIRECTION_NEUTRAL);

        Map<String, Object> detail = new HashMap<>();
        detail.put("orderNo", order.getOrderNo());
        detail.put("refundNo", order.getRefundNo());
        detail.put("refundAmount", order.getRefundAmount());
        detail.put("storeName", order.getStoreName());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getBenefitEvents(Long memberId, List<Integer> eventTypes,
                                                    LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<BenefitUseLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BenefitUseLog::getMemberId, memberId);
        if (startTime != null) {
            wrapper.and(w -> w.ge(BenefitUseLog::getLockTime, startTime)
                    .or().ge(BenefitUseLog::getConfirmTime, startTime)
                    .or().ge(BenefitUseLog::getReturnTime, startTime));
        }
        if (endTime != null) {
            wrapper.and(w -> w.le(BenefitUseLog::getLockTime, endTime)
                    .or().le(BenefitUseLog::getConfirmTime, endTime)
                    .or().le(BenefitUseLog::getReturnTime, endTime));
        }
        wrapper.orderByDesc(BenefitUseLog::getLockTime);
        wrapper.last("LIMIT " + limit);

        List<BenefitUseLog> logs = benefitUseLogMapper.selectList(wrapper);

        List<TimelineEventVO> events = new ArrayList<>();
        for (BenefitUseLog log : logs) {
            if (log.getLockTime() != null
                    && isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_LOCK)
                    && (startTime == null || !log.getLockTime().isBefore(startTime))
                    && (endTime == null || !log.getLockTime().isAfter(endTime))) {
                events.add(convertBenefitLock(log));
            }
            if (log.getConfirmTime() != null
                    && isEventTypeIncluded(eventTypes, TimelineEventTypeEnum.COUPON_USE)
                    && (startTime == null || !log.getConfirmTime().isBefore(startTime))
                    && (endTime == null || !log.getConfirmTime().isAfter(endTime))) {
                events.add(convertBenefitUse(log));
            }
        }
        return events;
    }

    private TimelineEventVO convertBenefitLock(BenefitUseLog log) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_LOCK, log.getUseNo() + "_LOCK"));
        vo.setEventType(TimelineEventTypeEnum.COUPON_LOCK.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.COUPON_LOCK.getName());
        vo.setEventTag(TimelineEventTypeEnum.COUPON_LOCK.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.COUPON_LOCK.getDesc());
        vo.setEventTime(log.getLockTime());
        vo.setBizType(BIZ_TYPE_COUPON);
        vo.setBizId(log.getOrderNo());
        vo.setAmount(log.getBenefitValue());
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(log.getOperator());

        Map<String, Object> detail = new HashMap<>();
        detail.put("useNo", log.getUseNo());
        detail.put("benefitType", log.getBenefitType());
        detail.put("benefitId", log.getBenefitId());
        detail.put("orderNo", log.getOrderNo());
        detail.put("orderAmount", log.getOrderAmount());
        detail.put("benefitValue", log.getBenefitValue());
        detail.put("usedPoints", log.getUsedPoints());
        detail.put("storeCode", log.getStoreCode());
        vo.setDetail(detail);

        return vo;
    }

    private TimelineEventVO convertBenefitUse(BenefitUseLog log) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.COUPON_USE, log.getUseNo()));
        vo.setEventType(TimelineEventTypeEnum.COUPON_USE.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.COUPON_USE.getName());
        vo.setEventTag(TimelineEventTypeEnum.COUPON_USE.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.COUPON_USE.getDesc());
        vo.setEventTime(log.getConfirmTime());
        vo.setBizType(BIZ_TYPE_COUPON);
        vo.setBizId(log.getOrderNo());
        vo.setAmount(log.getBenefitValue());
        vo.setPoints(log.getUsedPoints());
        vo.setDirection(DIRECTION_SUBTRACT);
        vo.setRelatedStaff(log.getOperator());

        Map<String, Object> detail = new HashMap<>();
        detail.put("useNo", log.getUseNo());
        detail.put("benefitType", log.getBenefitType());
        detail.put("benefitId", log.getBenefitId());
        detail.put("orderNo", log.getOrderNo());
        detail.put("orderAmount", log.getOrderAmount());
        detail.put("benefitValue", log.getBenefitValue());
        detail.put("usedPoints", log.getUsedPoints());
        detail.put("storeCode", log.getStoreCode());
        detail.put("remark", log.getRemark());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getMergeEvents(Long memberId, LocalDateTime startTime,
                                                  LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<MemberMergeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(MemberMergeLog::getSourceMemberId, memberId)
                .or().eq(MemberMergeLog::getTargetMemberId, memberId));
        if (startTime != null) {
            wrapper.ge(MemberMergeLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(MemberMergeLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(MemberMergeLog::getCreateTime);
        wrapper.last("LIMIT " + limit);

        List<MemberMergeLog> logs = memberMergeLogMapper.selectList(wrapper);

        return logs.stream()
                .map(log -> convertMergeLog(log, memberId))
                .collect(Collectors.toList());
    }

    private TimelineEventVO convertMergeLog(MemberMergeLog log, Long memberId) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.MEMBER_MERGE, log.getMergeNo()));
        vo.setEventType(TimelineEventTypeEnum.MEMBER_MERGE.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.MEMBER_MERGE.getName());
        vo.setEventTag(TimelineEventTypeEnum.MEMBER_MERGE.getTag());
        vo.setEventDesc(TimelineEventTypeEnum.MEMBER_MERGE.getDesc());
        vo.setEventTime(log.getCreateTime());
        vo.setBizType(BIZ_TYPE_MERGE);
        vo.setBizId(log.getMergeNo());
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(log.getOperator());

        boolean isSource = Objects.equals(log.getSourceMemberId(), memberId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("mergeNo", log.getMergeNo());
        detail.put("sourceMemberId", log.getSourceMemberId());
        detail.put("targetMemberId", log.getTargetMemberId());
        detail.put("mergedPoints", log.getMergedPoints());
        detail.put("mergedGrowth", log.getMergedGrowth());
        detail.put("mergedCoupons", log.getMergedCoupons());
        detail.put("mergeDirection", isSource ? "被合并" : "合并入");
        detail.put("reason", log.getReason());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getMessageEvents(Long memberId, LocalDateTime startTime,
                                                    LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLog::getMemberId, memberId);
        if (startTime != null) {
            wrapper.ge(MessageLog::getSendTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(MessageLog::getSendTime, endTime);
        }
        wrapper.orderByDesc(MessageLog::getSendTime);
        wrapper.last("LIMIT " + limit);

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        return logs.stream()
                .map(this::convertMessageLog)
                .collect(Collectors.toList());
    }

    private TimelineEventVO convertMessageLog(MessageLog log) {
        TimelineEventVO vo = new TimelineEventVO();
        vo.setEventId(generateEventId(TimelineEventTypeEnum.MESSAGE_PUSH, log.getMsgNo()));
        vo.setEventType(TimelineEventTypeEnum.MESSAGE_PUSH.getCode());
        vo.setEventTypeName(TimelineEventTypeEnum.MESSAGE_PUSH.getName());
        vo.setEventTag(TimelineEventTypeEnum.MESSAGE_PUSH.getTag());
        vo.setEventDesc(log.getMsgTitle());
        vo.setEventTime(log.getSendTime());
        vo.setBizType(BIZ_TYPE_MESSAGE);
        vo.setBizId(log.getMsgNo());
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(log.getChannel());

        Map<String, Object> detail = new HashMap<>();
        detail.put("msgNo", log.getMsgNo());
        detail.put("msgType", log.getMsgType());
        detail.put("msgTitle", log.getMsgTitle());
        detail.put("msgContent", log.getMsgContent());
        detail.put("channel", log.getChannel());
        detail.put("sendStatus", log.getSendStatus());
        detail.put("bizId", log.getBizId());
        detail.put("bizData", log.getBizData());
        vo.setDetail(detail);

        return vo;
    }

    private List<TimelineEventVO> getIdempotentEvents(Long memberId, List<Integer> eventTypes,
                                                       LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<IdempotentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdempotentRecord::getOperatorType, 1)
                .or()
                .eq(IdempotentRecord::getOperatorType, 2);
        wrapper.orderByDesc(IdempotentRecord::getOperateTime);
        wrapper.last("LIMIT " + limit);

        List<IdempotentRecord> records = idempotentRecordMapper.selectList(wrapper);

        List<TimelineEventVO> events = new ArrayList<>();
        for (IdempotentRecord record : records) {
            if (record.getOperatorType() == null || record.getOperatorType() == 0) {
                continue;
            }
            TimelineEventVO event = convertIdempotentRecord(record);
            if (startTime != null && event.getEventTime() != null && event.getEventTime().isBefore(startTime)) {
                continue;
            }
            if (endTime != null && event.getEventTime() != null && event.getEventTime().isAfter(endTime)) {
                continue;
            }
            if (CollectionUtils.isEmpty(eventTypes) || eventTypes.contains(event.getEventType())) {
                events.add(event);
            }
        }
        return events;
    }

    private TimelineEventVO convertIdempotentRecord(IdempotentRecord record) {
        TimelineEventVO vo = new TimelineEventVO();
        boolean isReplay = record.getOperatorType() != null && record.getOperatorType() == 1;
        TimelineEventTypeEnum eventType = isReplay
                ? TimelineEventTypeEnum.MANUAL_REPLAY
                : TimelineEventTypeEnum.MANUAL_MARK_FAIL;

        vo.setEventId(generateEventId(eventType, record.getId()));
        vo.setEventType(eventType.getCode());
        vo.setEventTypeName(eventType.getName());
        vo.setEventTag(eventType.getTag());
        vo.setEventDesc(eventType.getDesc());
        vo.setEventTime(record.getOperateTime());
        vo.setBizType(BIZ_TYPE_COUPON);
        vo.setBizId(record.getBusinessNo());
        vo.setDirection(DIRECTION_NEUTRAL);
        vo.setRelatedStaff(record.getOperator());

        Map<String, Object> detail = new HashMap<>();
        detail.put("idempotentRecordId", record.getId());
        detail.put("businessNo", record.getBusinessNo());
        detail.put("businessType", record.getBusinessType());
        detail.put("processStatus", record.getProcessStatus());
        detail.put("retryCount", record.getRetryCount());
        detail.put("remark", record.getRemark());
        vo.setDetail(detail);

        return vo;
    }

    private String generateEventId(TimelineEventTypeEnum eventType, Object id) {
        String typeCode = String.format("%02d", eventType.getCode());
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "EVT" + dateStr + typeCode + id;
    }
}
