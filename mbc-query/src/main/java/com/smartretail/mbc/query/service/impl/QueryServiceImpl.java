package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.enums.PointTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.coupon.vo.CouponInstanceVO;
import com.smartretail.mbc.coupon.vo.CouponTemplateVO;
import com.smartretail.mbc.level.entity.LevelRule;
import com.smartretail.mbc.level.mapper.LevelRuleMapper;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.point.entity.PointLog;
import com.smartretail.mbc.point.mapper.PointLogMapper;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.entity.Activity;
import com.smartretail.mbc.query.mapper.ActivityMapper;
import com.smartretail.mbc.query.mapper.QueryStatsMapper;
import com.smartretail.mbc.query.service.QueryService;
import com.smartretail.mbc.query.vo.ActivityStatsVO;
import com.smartretail.mbc.query.vo.ConsumeRecordVO;
import com.smartretail.mbc.query.vo.DashboardStatsVO;
import com.smartretail.mbc.query.vo.PersonalBenefitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final ActivityMapper activityMapper;
    private final QueryStatsMapper queryStatsMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final MemberMapper memberMapper;
    private final LevelRuleMapper levelRuleMapper;
    private final PointLogMapper pointLogMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;

    private static final BigDecimal PERCENT_100 = new BigDecimal("100");
    private static final BigDecimal COUPON_ESTIMATED_VALUE = new BigDecimal("10");

    @Override
    public IPage<ConsumeRecordVO> queryConsumeRecords(ConsumeRecordQueryDTO dto) {
        if (dto.getMemberId() == null) {
            throw new BusinessException("会员ID不能为空");
        }
        Page<ConsumeOrder> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ConsumeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumeOrder::getMemberId, dto.getMemberId())
                .in(ConsumeOrder::getOrderStatus, Arrays.asList(1, 2));
        if (dto.getStartTime() != null) {
            wrapper.ge(ConsumeOrder::getPayTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(ConsumeOrder::getPayTime, dto.getEndTime());
        }
        if (dto.getOrderType() != null) {
            wrapper.eq(ConsumeOrder::getOrderType, dto.getOrderType());
        }
        if (dto.getMinAmount() != null) {
            wrapper.ge(ConsumeOrder::getPayAmount, dto.getMinAmount());
        }
        if (dto.getMaxAmount() != null) {
            wrapper.le(ConsumeOrder::getPayAmount, dto.getMaxAmount());
        }
        if (StringUtils.hasText(dto.getStoreCode())) {
            wrapper.eq(ConsumeOrder::getStoreCode, dto.getStoreCode());
        }
        wrapper.orderByDesc(ConsumeOrder::getPayTime);

        IPage<ConsumeOrder> orderPage = consumeOrderMapper.selectPage(page, wrapper);
        IPage<ConsumeRecordVO> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<ConsumeRecordVO> voList = orderPage.getRecords().stream().map(this::convertToConsumeRecordVO).collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private ConsumeRecordVO convertToConsumeRecordVO(ConsumeOrder order) {
        ConsumeRecordVO vo = new ConsumeRecordVO();
        BeanUtils.copyProperties(order, vo);
        vo.setOrderTypeName(getOrderTypeName(order.getOrderType()));
        vo.setChannelName(getChannelName(order.getChannel()));
        vo.setUsedCoupons(countUsedCoupons(order.getUsedCouponIds()));
        return vo;
    }

    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "普通订单";
        switch (orderType) {
            case 1: return "普通订单";
            case 2: return "活动订单";
            case 3: return "退款订单";
            default: return "普通订单";
        }
    }

    private String getChannelName(String channel) {
        if (!StringUtils.hasText(channel)) return "线下门店";
        switch (channel) {
            case "ONLINE": return "线上商城";
            case "OFFLINE": return "线下门店";
            case "APP": return "APP下单";
            case "MINIAPP": return "小程序";
            default: return channel;
        }
    }

    private Integer countUsedCoupons(String usedCouponIds) {
        if (!StringUtils.hasText(usedCouponIds)) return 0;
        return (int) Arrays.stream(usedCouponIds.split(","))
                .filter(StringUtils::hasText).count();
    }

    @Override
    public PersonalBenefitVO getPersonalBenefitList(BenefitListQueryDTO dto) {
        if (dto.getMemberId() == null) {
            throw new BusinessException("会员ID不能为空");
        }
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        PersonalBenefitVO vo = new PersonalBenefitVO();
        vo.setMemberId(member.getId());
        vo.setMemberName(member.getName());
        vo.setLevelCode(member.getLevelCode());

        LevelRule levelRule = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>()
                        .eq(LevelRule::getLevelCode, member.getLevelCode())
                        .eq(LevelRule::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (levelRule != null) {
            vo.setLevelName(levelRule.getLevelName());
            vo.setLevelBenefits(parseBenefitDesc(levelRule.getBenefitDesc()));
        } else {
            MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
            vo.setLevelName(levelEnum.getName());
            vo.setLevelBenefits(new ArrayList<>());
        }

        PersonalBenefitVO.PointSummary pointSummary = buildPointSummary(member);
        vo.setPointInfo(pointSummary);

        PersonalBenefitVO.CouponSummary couponSummary = buildCouponSummary(member.getId(), dto.getIncludeExpired());
        vo.setCouponSummary(couponSummary);

        List<CouponInstanceVO> couponList = buildCouponList(member.getId(), dto.getIncludeExpired());
        vo.setCouponList(couponList);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Long recentCount = queryStatsMapper.countMemberConsume(member.getId(), thirtyDaysAgo, now);
        vo.setRecentConsumeCount(recentCount != null ? recentCount : 0L);

        Map<String, Object> totalConsume = queryStatsMapper.sumMemberConsume(member.getId(), null, null);
        if (totalConsume != null && totalConsume.get("totalAmount") != null) {
            vo.setTotalConsumeAmount(new BigDecimal(totalConsume.get("totalAmount").toString()));
        } else {
            vo.setTotalConsumeAmount(BigDecimal.ZERO);
        }

        return vo;
    }

    private List<String> parseBenefitDesc(String benefitDesc) {
        if (!StringUtils.hasText(benefitDesc)) {
            return new ArrayList<>();
        }
        return Arrays.stream(benefitDesc.split("[,，;；\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private PersonalBenefitVO.PointSummary buildPointSummary(Member member) {
        PersonalBenefitVO.PointSummary summary = new PersonalBenefitVO.PointSummary();
        summary.setCurrentPoints(member.getCurrentPoints() != null ? member.getCurrentPoints() : 0);

        List<PointLog> frozenLogs = pointLogMapper.selectList(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getMemberId, member.getId())
                        .eq(PointLog::getPointType, PointTypeEnum.FREEZE.getCode())
                        .eq(PointLog::getSourceType, PointTypeEnum.FREEZE.getCode())
        );
        int frozenPoints = frozenLogs.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        summary.setFrozenPoints(frozenPoints);

        Integer totalEarned = pointLogMapper.selectTotalByType(member.getId(), PointTypeEnum.ADD.getCode());
        summary.setTotalEarned(totalEarned != null ? totalEarned : 0);

        Integer totalUsed = pointLogMapper.selectTotalByType(member.getId(), PointTypeEnum.SUBTRACT.getCode());
        summary.setTotalUsed(totalUsed != null ? totalUsed : 0);

        List<PointLog> expiringPoints = pointLogMapper.selectExpiringPoints(member.getId(), 30);
        int expiringIn30Days = expiringPoints.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        summary.setExpiringIn30Days(expiringIn30Days);

        return summary;
    }

    private PersonalBenefitVO.CouponSummary buildCouponSummary(Long memberId, Boolean includeExpired) {
        PersonalBenefitVO.CouponSummary summary = new PersonalBenefitVO.CouponSummary();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in7Days = now.plus(7, ChronoUnit.DAYS);
        LocalDateTime in30Days = now.plus(30, ChronoUnit.DAYS);

        List<Integer> availableStatuses = includeExpired
                ? Arrays.asList(CouponStatusEnum.AVAILABLE.getCode(), CouponStatusEnum.NOT_STARTED.getCode(), CouponStatusEnum.EXPIRED.getCode())
                : Arrays.asList(CouponStatusEnum.AVAILABLE.getCode(), CouponStatusEnum.NOT_STARTED.getCode());

        List<CouponInstance> allCoupons = couponInstanceMapper.selectList(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getMemberId, memberId)
                        .in(CouponInstance::getCouponStatus, availableStatuses)
        );
        summary.setTotalAvailable(allCoupons.size());

        int expiring7 = (int) allCoupons.stream()
                .filter(c -> c.getValidEnd() != null && c.getValidEnd().isAfter(now) && c.getValidEnd().isBefore(in7Days))
                .count();
        summary.setExpiringIn7Days(expiring7);

        int expiring30 = (int) allCoupons.stream()
                .filter(c -> c.getValidEnd() != null && c.getValidEnd().isAfter(now) && c.getValidEnd().isBefore(in30Days))
                .count();
        summary.setExpiringIn30Days(expiring30);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        Long usedThisMonth = couponInstanceMapper.selectCount(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getMemberId, memberId)
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.USED.getCode())
                        .ge(CouponInstance::getUsedTime, monthStart)
                        .le(CouponInstance::getUsedTime, monthEnd)
        );
        summary.setUsedThisMonth(usedThisMonth != null ? usedThisMonth.intValue() : 0);

        return summary;
    }

    private List<CouponInstanceVO> buildCouponList(Long memberId, Boolean includeExpired) {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> statuses = includeExpired
                ? Arrays.asList(CouponStatusEnum.AVAILABLE.getCode(), CouponStatusEnum.NOT_STARTED.getCode(), CouponStatusEnum.EXPIRED.getCode())
                : Arrays.asList(CouponStatusEnum.AVAILABLE.getCode(), CouponStatusEnum.NOT_STARTED.getCode());

        Page<CouponInstance> page = new Page<>(1, 20);
        IPage<CouponInstance> couponPage = couponInstanceMapper.selectPage(page,
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getMemberId, memberId)
                        .in(CouponInstance::getCouponStatus, statuses)
                        .orderByAsc(CouponInstance::getValidEnd)
        );

        List<Long> templateIds = couponPage.getRecords().stream()
                .map(CouponInstance::getTemplateId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, CouponTemplateVO> templateMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(templateIds)) {
            List<CouponTemplate> templates = couponTemplateMapper.selectBatchIds(templateIds);
            for (CouponTemplate t : templates) {
                CouponTemplateVO vo = new CouponTemplateVO();
                BeanUtils.copyProperties(t, vo);
                templateMap.put(t.getId(), vo);
            }
        }

        return couponPage.getRecords().stream().map(ci -> {
            CouponInstanceVO vo = new CouponInstanceVO();
            BeanUtils.copyProperties(ci, vo);
            vo.setTemplate(templateMap.get(ci.getTemplateId()));
            if (ci.getValidEnd() != null) {
                long days = ChronoUnit.DAYS.between(now.toLocalDate(), ci.getValidEnd().toLocalDate());
                if (days <= 7 && days >= 0) {
                    vo.setProgressBarText("即将过期，剩余" + days + "天");
                } else if (days < 0) {
                    vo.setProgressBarText("已过期" + Math.abs(days) + "天");
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<ActivityStatsVO> queryActivityStats(ActivityStatsQueryDTO dto) {
        Page<Activity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (dto.getActivityId() != null) {
            wrapper.eq(Activity::getId, dto.getActivityId());
        }
        if (dto.getActivityType() != null) {
            wrapper.eq(Activity::getActivityType, dto.getActivityType());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(Activity::getStartTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(Activity::getEndTime, dto.getEndTime());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Activity::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(Activity::getCreateTime);

        IPage<Activity> activityPage = activityMapper.selectPage(page, wrapper);
        IPage<ActivityStatsVO> resultPage = new Page<>(activityPage.getCurrent(), activityPage.getSize(), activityPage.getTotal());
        List<ActivityStatsVO> voList = activityPage.getRecords().stream()
                .map(this::buildActivityStatsVO).collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private ActivityStatsVO buildActivityStatsVO(Activity activity) {
        ActivityStatsVO vo = new ActivityStatsVO();
        BeanUtils.copyProperties(activity, vo);

        ActivityStatsVO.BudgetUsage budgetUsage = new ActivityStatsVO.BudgetUsage();
        budgetUsage.setPointsUsedPercent(calcPercent(activity.getUsedPoints(), activity.getBudgetPoints()));
        budgetUsage.setCouponsUsedPercent(calcPercent(activity.getUsedCoupons(), activity.getBudgetCoupons()));
        vo.setBudgetUsage(budgetUsage);

        ActivityStatsVO.Funnel funnel = new ActivityStatsVO.Funnel();
        funnel.setExposureRate(calcPercent(activity.getParticipatedCount(), activity.getExposedCount()));
        funnel.setParticipationRate(calcPercent(activity.getParticipatedCount(), activity.getExposedCount()));
        funnel.setConversionRate(calcPercent(activity.getConvertedCount(), activity.getParticipatedCount()));
        vo.setFunnel(funnel);

        BigDecimal pointCost = activity.getUsedPoints() != null
                ? new BigDecimal(activity.getUsedPoints()).divide(PERCENT_100, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal couponCost = activity.getUsedCoupons() != null
                ? new BigDecimal(activity.getUsedCoupons()).multiply(COUPON_ESTIMATED_VALUE)
                : BigDecimal.ZERO;
        BigDecimal totalCost = pointCost.add(couponCost);
        BigDecimal drivenAmount = activity.getDrivenOrderAmount() != null ? activity.getDrivenOrderAmount() : BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            vo.setRoi(drivenAmount.divide(totalCost, 2, RoundingMode.HALF_UP));
        } else {
            vo.setRoi(BigDecimal.ZERO);
        }

        return vo;
    }

    private BigDecimal calcPercent(Number part, Number total) {
        if (part == null || total == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal partBd = new BigDecimal(part.toString());
        BigDecimal totalBd = new BigDecimal(total.toString());
        if (totalBd.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return partBd.multiply(PERCENT_100).divide(totalBd, 2, RoundingMode.HALF_UP);
    }

    @Override
    public ActivityStatsVO getActivityDetailStats(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        ActivityStatsVO vo = buildActivityStatsVO(activity);

        List<Map<String, Object>> levelMaps = queryStatsMapper.countActivityParticipantsByLevel(activityId);
        Map<String, Integer> participantLevels = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(levelMaps)) {
            for (Map<String, Object> m : levelMaps) {
                String key = m.get("levelCode") != null ? m.get("levelCode").toString() : "0";
                int count = m.get("participantCount") != null ? Integer.parseInt(m.get("participantCount").toString()) : 0;
                participantLevels.put(key, count);
            }
        }
        vo.setParticipantLevels(participantLevels);

        List<Map<String, Object>> trendMaps = queryStatsMapper.selectActivityDailyTrend(activityId);
        List<ActivityStatsVO.DailyDataItem> dailyTrend = new ArrayList<>();
        if (!CollectionUtils.isEmpty(trendMaps)) {
            for (Map<String, Object> m : trendMaps) {
                ActivityStatsVO.DailyDataItem item = new ActivityStatsVO.DailyDataItem();
                Object dateObj = m.get("statDate");
                if (dateObj != null) {
                    if (dateObj instanceof LocalDate) {
                        item.setDate((LocalDate) dateObj);
                    } else if (dateObj instanceof Date) {
                        item.setDate(((Date) dateObj).toLocalDate());
                    } else {
                        item.setDate(LocalDate.parse(dateObj.toString()));
                    }
                }
                item.setNewParticipants(m.get("newParticipants") != null ? Integer.parseInt(m.get("newParticipants").toString()) : 0);
                item.setNewConversions(m.get("newConversions") != null ? Integer.parseInt(m.get("newConversions").toString()) : 0);
                item.setOrderAmount(m.get("orderAmount") != null ? new BigDecimal(m.get("orderAmount").toString()) : BigDecimal.ZERO);
                dailyTrend.add(item);
            }
        }
        vo.setDailyTrend(dailyTrend);

        return vo;
    }

    @Override
    public DashboardStatsVO getDashboardStats(DashboardStatsDTO dto) {
        DashboardStatsVO vo = new DashboardStatsVO();
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startTime = dto.getStartTime() != null ? dto.getStartTime() : currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = dto.getEndTime() != null ? dto.getEndTime() : now;

        DashboardStatsVO.MemberStats memberStats = buildMemberStats(startTime, endTime);
        vo.setMemberStats(memberStats);

        DashboardStatsVO.OrderStats orderStats = buildOrderStats(startTime, endTime, dto.getStoreCode());
        vo.setOrderStats(orderStats);

        DashboardStatsVO.BenefitStats benefitStats = buildBenefitStats(startTime, endTime);
        vo.setBenefitStats(benefitStats);

        List<ActivityStatsVO> topActivities = buildTopActivities();
        vo.setTopActivities(topActivities);

        List<DashboardStatsVO.LevelDistributionItem> levelDistribution = buildLevelDistribution();
        vo.setLevelDistribution(levelDistribution);

        return vo;
    }

    private DashboardStatsVO.MemberStats buildMemberStats(LocalDateTime start, LocalDateTime end) {
        DashboardStatsVO.MemberStats stats = new DashboardStatsVO.MemberStats();
        Long totalMembers = memberMapper.selectCount(null);
        stats.setTotalMembers(totalMembers != null ? totalMembers : 0L);

        Long newThisMonth = queryStatsMapper.countNewMembersBetween(start, end);
        stats.setNewThisMonth(newThisMonth != null ? newThisMonth : 0L);

        Long activeThisMonth = queryStatsMapper.countActiveMembersBetween(start, end);
        stats.setActiveThisMonth(activeThisMonth != null ? activeThisMonth : 0L);

        List<Map<String, Object>> byLevelList = queryStatsMapper.countMembersByLevel();
        Map<String, Long> byLevel = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(byLevelList)) {
            for (Map<String, Object> m : byLevelList) {
                String key = m.get("levelCode") != null ? m.get("levelCode").toString() : "0";
                Long count = m.get("memberCount") != null ? Long.parseLong(m.get("memberCount").toString()) : 0L;
                byLevel.put(key, count);
            }
        }
        stats.setByLevel(byLevel);

        return stats;
    }

    private DashboardStatsVO.OrderStats buildOrderStats(LocalDateTime start, LocalDateTime end, String storeCode) {
        DashboardStatsVO.OrderStats stats = new DashboardStatsVO.OrderStats();
        Map<String, Object> orderMap = queryStatsMapper.sumOrdersBetween(start, end, storeCode);

        Long totalOrders = orderMap != null && orderMap.get("orderCount") != null
                ? Long.parseLong(orderMap.get("orderCount").toString()) : 0L;
        stats.setTotalOrders(totalOrders);

        BigDecimal totalPayAmount = orderMap != null && orderMap.get("totalPayAmount") != null
                ? new BigDecimal(orderMap.get("totalPayAmount").toString()) : BigDecimal.ZERO;
        stats.setTotalPayAmount(totalPayAmount);

        if (totalOrders > 0) {
            stats.setAvgOrderAmount(totalPayAmount.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP));
        } else {
            stats.setAvgOrderAmount(BigDecimal.ZERO);
        }

        Long memberOrderCount = orderMap != null && orderMap.get("memberOrderCount") != null
                ? Long.parseLong(orderMap.get("memberOrderCount").toString()) : 0L;
        if (totalOrders > 0) {
            stats.setMemberOrderRatio(new BigDecimal(memberOrderCount).multiply(PERCENT_100)
                    .divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP));
        } else {
            stats.setMemberOrderRatio(BigDecimal.ZERO);
        }

        return stats;
    }

    private DashboardStatsVO.BenefitStats buildBenefitStats(LocalDateTime start, LocalDateTime end) {
        DashboardStatsVO.BenefitStats stats = new DashboardStatsVO.BenefitStats();

        Long totalIssued = couponInstanceMapper.selectCount(
                new LambdaQueryWrapper<CouponInstance>()
                        .ge(CouponInstance::getReceiveTime, start)
                        .le(CouponInstance::getReceiveTime, end)
        );
        stats.setTotalCouponsIssued(totalIssued != null ? totalIssued : 0L);

        Long totalUsed = couponInstanceMapper.selectCount(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.USED.getCode())
                        .ge(CouponInstance::getUsedTime, start)
                        .le(CouponInstance::getUsedTime, end)
        );
        stats.setTotalCouponsUsed(totalUsed != null ? totalUsed : 0L);

        if (totalIssued != null && totalIssued > 0) {
            stats.setCouponUsageRate(new BigDecimal(totalUsed).multiply(PERCENT_100)
                    .divide(new BigDecimal(totalIssued), 2, RoundingMode.HALF_UP));
        } else {
            stats.setCouponUsageRate(BigDecimal.ZERO);
        }

        List<PointLog> addLogs = pointLogMapper.selectList(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getPointType, PointTypeEnum.ADD.getCode())
                        .ge(PointLog::getCreateTime, start)
                        .le(PointLog::getCreateTime, end)
        );
        long totalPointsIssued = addLogs.stream()
                .mapToLong(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        stats.setTotalPointsIssued(totalPointsIssued);

        List<PointLog> subLogs = pointLogMapper.selectList(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getPointType, PointTypeEnum.SUBTRACT.getCode())
                        .ge(PointLog::getCreateTime, start)
                        .le(PointLog::getCreateTime, end)
        );
        long totalPointsRedeemed = subLogs.stream()
                .mapToLong(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        stats.setTotalPointsRedeemed(totalPointsRedeemed);

        return stats;
    }

    private List<ActivityStatsVO> buildTopActivities() {
        Page<Activity> page = new Page<>(1, 5);
        IPage<Activity> topPage = activityMapper.selectPage(page,
                new LambdaQueryWrapper<Activity>()
                        .orderByDesc(Activity::getDrivenOrderAmount)
        );
        return topPage.getRecords().stream()
                .map(this::buildActivityStatsVO).collect(Collectors.toList());
    }

    private List<DashboardStatsVO.LevelDistributionItem> buildLevelDistribution() {
        List<DashboardStatsVO.LevelDistributionItem> result = new ArrayList<>();
        List<Map<String, Object>> levelMaps = queryStatsMapper.countMembersByLevel();
        long total = 0;
        Map<Integer, Long> levelCountMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(levelMaps)) {
            for (Map<String, Object> m : levelMaps) {
                Integer code = m.get("levelCode") != null ? Integer.parseInt(m.get("levelCode").toString()) : 0;
                Long count = m.get("memberCount") != null ? Long.parseLong(m.get("memberCount").toString()) : 0L;
                levelCountMap.put(code, count);
                total += count;
            }
        }
        for (MemberLevelEnum level : MemberLevelEnum.values()) {
            DashboardStatsVO.LevelDistributionItem item = new DashboardStatsVO.LevelDistributionItem();
            item.setLevelCode(level.getCode());
            item.setLevelName(level.getName());
            Long count = levelCountMap.getOrDefault(level.getCode(), 0L);
            item.setCount(count);
            if (total > 0) {
                item.setRatio(new BigDecimal(count).multiply(PERCENT_100)
                        .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP));
            } else {
                item.setRatio(BigDecimal.ZERO);
            }
            result.add(item);
        }
        return result;
    }
}
