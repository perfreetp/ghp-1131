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
import com.smartretail.mbc.query.dto.ActivityCreateDTO;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.ActivityStatusDTO;
import com.smartretail.mbc.query.dto.ActivityUpdateDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.dto.MiniBenefitQueryDTO;
import com.smartretail.mbc.query.entity.Activity;
import com.smartretail.mbc.query.entity.CrowdGroup;
import com.smartretail.mbc.query.entity.CrowdMember;
import com.smartretail.mbc.query.mapper.ActivityMapper;
import com.smartretail.mbc.query.mapper.CrowdGroupMapper;
import com.smartretail.mbc.query.mapper.CrowdMemberMapper;
import com.smartretail.mbc.query.mapper.QueryStatsMapper;
import com.smartretail.mbc.query.service.QueryService;
import com.smartretail.mbc.query.vo.ActivityEffectDetailVO;
import com.smartretail.mbc.query.vo.ActivityStatsVO;
import com.smartretail.mbc.query.vo.ConsumeRecordVO;
import com.smartretail.mbc.query.vo.CrowdEffectCompareVO;
import com.smartretail.mbc.query.vo.DashboardStatsVO;
import com.smartretail.mbc.query.vo.ExpireReminderVO;
import com.smartretail.mbc.query.vo.LevelBenefitItemVO;
import com.smartretail.mbc.query.vo.MiniBirthdayVO;
import com.smartretail.mbc.query.vo.MiniConsumeStatsVO;
import com.smartretail.mbc.query.vo.MiniCouponVO;
import com.smartretail.mbc.query.vo.MiniMemberCardVO;
import com.smartretail.mbc.query.vo.MiniPersonalBenefitVO;
import com.smartretail.mbc.query.vo.MiniPointVO;
import com.smartretail.mbc.query.vo.PersonalBenefitVO;
import com.smartretail.mbc.common.result.PageResult;
import com.smartretail.mbc.common.enums.CouponTypeEnum;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final CrowdGroupMapper crowdGroupMapper;
    private final CrowdMemberMapper crowdMemberMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final BigDecimal PERCENT_100 = new BigDecimal("100");
    private static final BigDecimal COUPON_ESTIMATED_VALUE = new BigDecimal("10");
    private static final String BIRTHDAY_KEY_PREFIX = "mbc:birthday:";
    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

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

    @Override
    public Long createActivity(ActivityCreateDTO dto) {
        if (dto.getActivityType() == null) {
            throw new BusinessException("活动类型不能为空");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        Long existCount = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getActivityCode, dto.getActivityCode())
        );
        if (existCount != null && existCount > 0) {
            throw new BusinessException("活动编码已存在");
        }
        validateActivityTypeFields(dto.getActivityType(), dto);

        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        activity.setRuleConfig(buildRuleConfig(dto));
        if (dto.getTargetLevel() == null) {
            activity.setTargetLevel(0);
        }
        if (dto.getStatus() == null) {
            activity.setStatus(0);
        }
        activity.setUsedPoints(0);
        activity.setUsedCoupons(0);
        activity.setExposedCount(0);
        activity.setParticipatedCount(0);
        activity.setConvertedCount(0);
        activity.setDrivenOrderAmount(BigDecimal.ZERO);
        activity.setDrivenOrderCount(0);
        activityMapper.insert(activity);
        return activity.getId();
    }

    private void validateActivityTypeFields(Integer activityType, ActivityCreateDTO dto) {
        switch (activityType) {
            case 1:
                if (CollectionUtils.isEmpty(dto.getCouponTemplateIds())) {
                    throw new BusinessException("发券活动必须配置关联券模板ID");
                }
                break;
            case 2:
                if (dto.getPointPerOrder() == null || dto.getPointPerOrder() <= 0) {
                    throw new BusinessException("积分活动必须配置每单送积分且大于0");
                }
                break;
            case 3:
                if (dto.getGrowthMultiplier() == null || dto.getGrowthMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("等级活动必须配置成长值倍率且大于0");
                }
                break;
            case 4:
                if (dto.getBirthdayPoints() == null && CollectionUtils.isEmpty(dto.getBirthdayCouponIds())) {
                    throw new BusinessException("生日礼活动必须配置送积分或送券");
                }
                break;
            case 5:
                if (dto.getPointMultiplier() == null || dto.getPointMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("积分翻倍活动必须配置倍率且大于0");
                }
                break;
            default:
                throw new BusinessException("不支持的活动类型");
        }
    }

    private String buildRuleConfig(ActivityCreateDTO dto) {
        try {
            Map<String, Object> ruleMap = new HashMap<>();
            if (StringUtils.hasText(dto.getApplyScenes())) {
                ruleMap.put("applyScenes", dto.getApplyScenes());
            }
            if (!CollectionUtils.isEmpty(dto.getCouponTemplateIds())) {
                ruleMap.put("couponTemplateIds", dto.getCouponTemplateIds());
            }
            if (dto.getPointMultiplier() != null) {
                ruleMap.put("pointMultiplier", dto.getPointMultiplier());
            }
            if (dto.getPointPerOrder() != null) {
                ruleMap.put("pointPerOrder", dto.getPointPerOrder());
            }
            if (dto.getBirthdayPoints() != null) {
                ruleMap.put("birthdayPoints", dto.getBirthdayPoints());
            }
            if (!CollectionUtils.isEmpty(dto.getBirthdayCouponIds())) {
                ruleMap.put("birthdayCouponIds", dto.getBirthdayCouponIds());
            }
            if (dto.getGrowthMultiplier() != null) {
                ruleMap.put("growthMultiplier", dto.getGrowthMultiplier());
            }
            if (StringUtils.hasText(dto.getRuleConfig())) {
                ruleMap.put("extra", dto.getRuleConfig());
            }
            return JSON.toJSONString(ruleMap);
        } catch (Exception e) {
            log.error("构建活动规则配置失败", e);
            return dto.getRuleConfig();
        }
    }

    @Override
    public void updateActivity(ActivityUpdateDTO dto) {
        if (dto.getActivityId() == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        boolean isRunning = activity.getStatus() != null && activity.getStatus() == 1;

        if (isRunning) {
            if (dto.getActivityType() != null || dto.getStartTime() != null
                    || dto.getEndTime() != null || dto.getCouponTemplateIds() != null
                    || dto.getPointMultiplier() != null || dto.getPointPerOrder() != null
                    || dto.getBirthdayPoints() != null || dto.getBirthdayCouponIds() != null
                    || dto.getGrowthMultiplier() != null) {
                throw new BusinessException("进行中的活动不能修改类型、时间和核心配置字段");
            }
        }

        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (!dto.getStartTime().isBefore(dto.getEndTime())) {
                throw new BusinessException("开始时间必须早于结束时间");
            }
        } else if (dto.getStartTime() != null) {
            if (!dto.getStartTime().isBefore(activity.getEndTime())) {
                throw new BusinessException("开始时间必须早于结束时间");
            }
        } else if (dto.getEndTime() != null) {
            if (!activity.getStartTime().isBefore(dto.getEndTime())) {
                throw new BusinessException("开始时间必须早于结束时间");
            }
        }

        if (dto.getActivityCode() != null) {
            Long existCount = activityMapper.selectCount(
                    new LambdaQueryWrapper<Activity>()
                            .eq(Activity::getActivityCode, dto.getActivityCode())
                            .ne(Activity::getId, dto.getActivityId())
            );
            if (existCount != null && existCount > 0) {
                throw new BusinessException("活动编码已存在");
            }
        }

        Activity update = new Activity();
        update.setId(dto.getActivityId());
        BeanUtils.copyProperties(dto, update);
        if (dto.getApplyScenes() != null || dto.getCouponTemplateIds() != null
                || dto.getPointMultiplier() != null || dto.getPointPerOrder() != null
                || dto.getBirthdayPoints() != null || dto.getBirthdayCouponIds() != null
                || dto.getGrowthMultiplier() != null || dto.getRuleConfig() != null) {
            ActivityCreateDTO createDTO = new ActivityCreateDTO();
            BeanUtils.copyProperties(dto, createDTO);
            if (dto.getApplyScenes() == null) {
                Map<String, Object> existRule = parseRuleConfig(activity.getRuleConfig());
                if (existRule.containsKey("applyScenes")) {
                    createDTO.setApplyScenes(existRule.get("applyScenes").toString());
                }
            }
            update.setRuleConfig(buildRuleConfig(createDTO));
        }
        activityMapper.updateById(update);
    }

    private Map<String, Object> parseRuleConfig(String ruleConfig) {
        if (!StringUtils.hasText(ruleConfig)) {
            return new HashMap<>();
        }
        try {
            return JSON.parseObject(ruleConfig, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析活动规则配置失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public void changeStatus(ActivityStatusDTO dto) {
        if (dto.getActivityId() == null) {
            throw new BusinessException("活动ID不能为空");
        }
        if (dto.getTargetStatus() == null) {
            throw new BusinessException("目标状态不能为空");
        }
        Activity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        Integer currentStatus = activity.getStatus() != null ? activity.getStatus() : 0;
        Integer targetStatus = dto.getTargetStatus();

        if (currentStatus.equals(targetStatus)) {
            return;
        }
        if (currentStatus == 2 && targetStatus == 1) {
            throw new BusinessException("已结束的活动不能重新进行中");
        }
        if (currentStatus == 3 && targetStatus != 3) {
            throw new BusinessException("已取消的活动不能变更状态");
        }
        if (targetStatus == 1 && currentStatus == 0) {
            if (activity.getStartTime() == null || activity.getEndTime() == null) {
                throw new BusinessException("请先配置活动时间");
            }
            if (activity.getActivityType() == null) {
                throw new BusinessException("请先配置活动类型");
            }
            if (!StringUtils.hasText(activity.getRuleConfig()) && (activity.getBudgetPoints() == null || activity.getBudgetPoints() == 0)
                    && (activity.getBudgetCoupons() == null || activity.getBudgetCoupons() == 0)) {
                throw new BusinessException("请完善活动配置后再发布");
            }
        }

        Activity update = new Activity();
        update.setId(dto.getActivityId());
        update.setStatus(targetStatus);
        if (targetStatus == 2) {
            update.setEndTime(LocalDateTime.now());
        }
        activityMapper.updateById(update);
    }

    @Override
    public ActivityEffectDetailVO getActivityEffectDetail(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        ActivityEffectDetailVO vo = new ActivityEffectDetailVO();
        BeanUtils.copyProperties(activity, vo);

        ActivityEffectDetailVO.EffectSummary summary = buildEffectSummary(activity);
        vo.setEffectSummary(summary);

        List<ActivityEffectDetailVO.CouponEffectVO> couponEffects = buildCouponEffects(activityId);
        vo.setCouponEffect(couponEffects);

        Map<String, ActivityEffectDetailVO.LevelEffect> levelEffect = buildLevelEffects(activityId);
        vo.setMemberLevelEffect(levelEffect);

        List<ActivityEffectDetailVO.DailyEffectItem> dailyTrend = buildDailyTrend(activityId);
        vo.setDailyTrend(dailyTrend);

        ActivityEffectDetailVO.RefundImpact refundImpact = buildRefundImpact(activityId, activity.getStartTime(), activity.getEndTime());
        vo.setRefundImpact(refundImpact);

        ActivityEffectDetailVO.CrowdInfo crowdInfo = buildCrowdInfo(activity.getCrowdGroupId());
        vo.setCrowdInfo(crowdInfo);

        List<CrowdEffectCompareVO> crowdEffectList = buildCrowdEffectList(activity);
        vo.setCrowdEffectList(crowdEffectList);

        return vo;
    }

    private ActivityEffectDetailVO.EffectSummary buildEffectSummary(Activity activity) {
        ActivityEffectDetailVO.EffectSummary summary = new ActivityEffectDetailVO.EffectSummary();
        Integer receiveCount = activity.getParticipatedCount() != null ? activity.getParticipatedCount() : 0;
        Integer verifyCount = activity.getConvertedCount() != null ? activity.getConvertedCount() : 0;
        Integer exposedCount = activity.getExposedCount() != null ? activity.getExposedCount() : 0;

        summary.setReceiveCount(receiveCount);
        summary.setReceiveCountRate(calcPercent(receiveCount, exposedCount));
        summary.setVerifyCount(verifyCount);
        summary.setVerifyCountRate(calcPercent(verifyCount, receiveCount));

        Long newMembers = queryStatsMapper.countActivityNewMembers(activity.getId(), activity.getStartTime(), activity.getEndTime());
        summary.setNewMemberCount(newMembers != null ? newMembers : 0L);

        Long totalOrderCount = activity.getDrivenOrderCount() != null ? activity.getDrivenOrderCount().longValue() : 0L;
        BigDecimal totalOrderAmount = activity.getDrivenOrderAmount() != null ? activity.getDrivenOrderAmount() : BigDecimal.ZERO;
        summary.setTotalOrderCount(totalOrderCount);
        summary.setTotalOrderAmount(totalOrderAmount);

        Map<String, Object> refundMap = queryStatsMapper.selectActivityRefundImpact(activity.getId(), activity.getStartTime(), activity.getEndTime());
        Long refundOrderCount = refundMap != null && refundMap.get("totalRefundCount") != null
                ? Long.parseLong(refundMap.get("totalRefundCount").toString()) : 0L;
        BigDecimal refundOrderAmount = refundMap != null && refundMap.get("totalRefundAmount") != null
                ? new BigDecimal(refundMap.get("totalRefundAmount").toString()) : BigDecimal.ZERO;
        summary.setRefundOrderCount(refundOrderCount);
        summary.setRefundOrderAmount(refundOrderAmount);
        summary.setRefundRatio(calcPercent(refundOrderCount, totalOrderCount));

        BigDecimal netOrderAmount = totalOrderAmount.subtract(refundOrderAmount);
        if (netOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            netOrderAmount = BigDecimal.ZERO;
        }
        summary.setNetOrderAmount(netOrderAmount);

        Integer costPoints = activity.getUsedPoints() != null ? activity.getUsedPoints() : 0;
        Integer costCouponCount = activity.getUsedCoupons() != null ? activity.getUsedCoupons() : 0;
        summary.setCostPoints(costPoints);
        summary.setCostCouponCount(costCouponCount);

        BigDecimal pointCost = new BigDecimal(costPoints).divide(PERCENT_100, 2, RoundingMode.HALF_UP);
        BigDecimal couponCost = new BigDecimal(costCouponCount).multiply(COUPON_ESTIMATED_VALUE);
        BigDecimal totalCost = pointCost.add(couponCost);
        summary.setTotalCost(totalCost);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            summary.setRoi(netOrderAmount.divide(totalCost, 2, RoundingMode.HALF_UP));
        } else {
            summary.setRoi(BigDecimal.ZERO);
        }

        return summary;
    }

    private List<ActivityEffectDetailVO.CouponEffectVO> buildCouponEffects(Long activityId) {
        List<ActivityEffectDetailVO.CouponEffectVO> result = new ArrayList<>();
        List<Map<String, Object>> maps = new ArrayList<>();
        Map<String, Object> couponMap = queryStatsMapper.selectActivityCouponEffect(activityId);
        if (couponMap != null && !couponMap.isEmpty() && couponMap.get("templateId") != null) {
            maps.add(couponMap);
        } else {
            List<CouponTemplate> templates = couponTemplateMapper.selectList(
                    new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getActivityId, activityId)
            );
            for (CouponTemplate t : templates) {
                Map<String, Object> m = new HashMap<>();
                m.put("templateId", t.getId());
                m.put("couponName", t.getCouponName());
                m.put("issuedCount", t.getReceivedCount() != null ? t.getReceivedCount() : 0);
                m.put("usedCount", t.getUsedCount() != null ? t.getUsedCount() : 0);
                m.put("unusedCount", (t.getReceivedCount() != null ? t.getReceivedCount() : 0) - (t.getUsedCount() != null ? t.getUsedCount() : 0));
                m.put("expiredCount", 0);
                m.put("usedOrderAmount", BigDecimal.ZERO);
                maps.add(m);
            }
        }
        for (Map<String, Object> m : maps) {
            ActivityEffectDetailVO.CouponEffectVO vo = new ActivityEffectDetailVO.CouponEffectVO();
            vo.setTemplateId(m.get("templateId") != null ? Long.parseLong(m.get("templateId").toString()) : null);
            vo.setCouponName(m.get("couponName") != null ? m.get("couponName").toString() : "");
            Integer issuedCount = m.get("issuedCount") != null ? Integer.parseInt(m.get("issuedCount").toString()) : 0;
            Integer usedCount = m.get("usedCount") != null ? Integer.parseInt(m.get("usedCount").toString()) : 0;
            Integer unusedCount = m.get("unusedCount") != null ? Integer.parseInt(m.get("unusedCount").toString()) : 0;
            Integer expiredCount = m.get("expiredCount") != null ? Integer.parseInt(m.get("expiredCount").toString()) : 0;
            vo.setIssuedCount(issuedCount);
            vo.setUsedCount(usedCount);
            vo.setUnusedCount(unusedCount);
            vo.setExpiredCount(expiredCount);
            vo.setUsedOrderAmount(m.get("usedOrderAmount") != null ? new BigDecimal(m.get("usedOrderAmount").toString()) : BigDecimal.ZERO);
            vo.setUsageRate(calcPercent(usedCount, issuedCount));
            result.add(vo);
        }
        return result;
    }

    private Map<String, ActivityEffectDetailVO.LevelEffect> buildLevelEffects(Long activityId) {
        Map<String, ActivityEffectDetailVO.LevelEffect> result = new LinkedHashMap<>();
        List<Map<String, Object>> levelMaps = queryStatsMapper.selectActivityMemberLevels(activityId);
        if (CollectionUtils.isEmpty(levelMaps)) {
            return result;
        }
        for (Map<String, Object> m : levelMaps) {
            String levelCode = m.get("levelCode") != null ? m.get("levelCode").toString() : "0";
            ActivityEffectDetailVO.LevelEffect effect = new ActivityEffectDetailVO.LevelEffect();
            effect.setLevelCode(levelCode);
            MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(Integer.parseInt(levelCode));
            effect.setLevelName(levelEnum.getName());
            effect.setParticipateCount(m.get("participateCount") != null ? Integer.parseInt(m.get("participateCount").toString()) : 0);
            effect.setVerifyCount(m.get("verifyCount") != null ? Integer.parseInt(m.get("verifyCount").toString()) : 0);
            effect.setOrderAmount(m.get("orderAmount") != null ? new BigDecimal(m.get("orderAmount").toString()) : BigDecimal.ZERO);
            result.put(levelCode, effect);
        }
        return result;
    }

    private List<ActivityEffectDetailVO.DailyEffectItem> buildDailyTrend(Long activityId) {
        List<ActivityEffectDetailVO.DailyEffectItem> result = new ArrayList<>();
        List<Map<String, Object>> trendMaps = queryStatsMapper.selectActivityDailyDetail(activityId);
        if (CollectionUtils.isEmpty(trendMaps)) {
            return result;
        }
        for (Map<String, Object> m : trendMaps) {
            ActivityEffectDetailVO.DailyEffectItem item = new ActivityEffectDetailVO.DailyEffectItem();
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
            item.setNewReceive(m.get("newReceive") != null ? Integer.parseInt(m.get("newReceive").toString()) : 0);
            item.setNewVerify(m.get("newVerify") != null ? Integer.parseInt(m.get("newVerify").toString()) : 0);
            item.setOrderCount(m.get("orderCount") != null ? Integer.parseInt(m.get("orderCount").toString()) : 0);
            item.setOrderAmount(m.get("orderAmount") != null ? new BigDecimal(m.get("orderAmount").toString()) : BigDecimal.ZERO);
            item.setRefundCount(m.get("refundCount") != null ? Integer.parseInt(m.get("refundCount").toString()) : 0);
            item.setRefundAmount(m.get("refundAmount") != null ? new BigDecimal(m.get("refundAmount").toString()) : BigDecimal.ZERO);
            result.add(item);
        }
        return result;
    }

    private ActivityEffectDetailVO.RefundImpact buildRefundImpact(Long activityId, LocalDateTime start, LocalDateTime end) {
        ActivityEffectDetailVO.RefundImpact impact = new ActivityEffectDetailVO.RefundImpact();
        Map<String, Object> refundMap = queryStatsMapper.selectActivityRefundImpact(activityId, start, end);
        Long totalRefundCount = refundMap != null && refundMap.get("totalRefundCount") != null
                ? Long.parseLong(refundMap.get("totalRefundCount").toString()) : 0L;
        BigDecimal totalRefundAmount = refundMap != null && refundMap.get("totalRefundAmount") != null
                ? new BigDecimal(refundMap.get("totalRefundAmount").toString()) : BigDecimal.ZERO;
        impact.setTotalRefundCount(totalRefundCount);
        impact.setTotalRefundAmount(totalRefundAmount);
        impact.setRefundByReason(new HashMap<>());
        return impact;
    }

    private ActivityEffectDetailVO.CrowdInfo buildCrowdInfo(Long crowdGroupId) {
        if (crowdGroupId == null) {
            return null;
        }
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(crowdGroupId);
        if (crowdGroup == null) {
            return null;
        }
        ActivityEffectDetailVO.CrowdInfo info = new ActivityEffectDetailVO.CrowdInfo();
        info.setCrowdId(crowdGroup.getId());
        info.setCrowdName(crowdGroup.getCrowdName());
        info.setTotalCount(crowdGroup.getActualCount() != null ? crowdGroup.getActualCount() : 0);
        return info;
    }

    private List<CrowdEffectCompareVO> buildCrowdEffectList(Activity activity) {
        List<CrowdEffectCompareVO> result = new ArrayList<>();
        if (activity.getCrowdGroupId() == null) {
            return result;
        }
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(activity.getCrowdGroupId());
        if (crowdGroup == null) {
            return result;
        }

        CrowdEffectCompareVO vo = new CrowdEffectCompareVO();
        vo.setCrowdId(crowdGroup.getId());
        vo.setCrowdName(crowdGroup.getCrowdName());

        Integer memberCount = crowdGroup.getActualCount() != null ? crowdGroup.getActualCount() : 0;
        vo.setMemberCount(memberCount);

        List<Long> memberIds = crowdMemberMapper.selectMemberIdsByCrowdId(crowdGroup.getId(), null);
        Set<Long> memberIdSet = new HashSet<>(memberIds);

        Integer participateCount = activity.getParticipatedCount() != null ? activity.getParticipatedCount() : 0;
        Integer verifyCount = activity.getConvertedCount() != null ? activity.getConvertedCount() : 0;
        vo.setParticipateCount(participateCount);
        vo.setVerifyCount(verifyCount);

        if (memberCount > 0) {
            vo.setParticipateRate(calcPercent(participateCount, memberCount));
            vo.setVerifyRate(calcPercent(verifyCount, memberCount));
        } else {
            vo.setParticipateRate(BigDecimal.ZERO);
            vo.setVerifyRate(BigDecimal.ZERO);
        }

        Integer orderCount = activity.getDrivenOrderCount() != null ? activity.getDrivenOrderCount() : 0;
        BigDecimal orderAmount = activity.getDrivenOrderAmount() != null ? activity.getDrivenOrderAmount() : BigDecimal.ZERO;
        vo.setOrderCount(orderCount);
        vo.setOrderAmount(orderAmount);

        if (orderCount > 0) {
            vo.setAvgOrderAmount(orderAmount.divide(new BigDecimal(orderCount), 2, RoundingMode.HALF_UP));
        } else {
            vo.setAvgOrderAmount(BigDecimal.ZERO);
        }

        Integer costPoints = activity.getUsedPoints() != null ? activity.getUsedPoints() : 0;
        Integer costCouponCount = activity.getUsedCoupons() != null ? activity.getUsedCoupons() : 0;
        BigDecimal pointCost = new BigDecimal(costPoints).divide(PERCENT_100, 2, RoundingMode.HALF_UP);
        BigDecimal couponCost = new BigDecimal(costCouponCount).multiply(COUPON_ESTIMATED_VALUE);
        BigDecimal totalCost = pointCost.add(couponCost);

        if (memberCount > 0) {
            vo.setCostValue(totalCost.divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP));
        } else {
            vo.setCostValue(BigDecimal.ZERO);
        }

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            vo.setRoi(orderAmount.divide(totalCost, 2, RoundingMode.HALF_UP));
        } else {
            vo.setRoi(BigDecimal.ZERO);
        }

        result.add(vo);
        return result;
    }

    @Override
    public IPage<ActivityEffectDetailVO> pageActivityEffect(ActivityStatsQueryDTO dto) {
        Page<Activity> page = new Page<>(dto.getPageNum() != null ? dto.getPageNum() : 1,
                dto.getPageSize() != null ? dto.getPageSize() : 10);
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
        IPage<ActivityEffectDetailVO> resultPage = new Page<>(activityPage.getCurrent(), activityPage.getSize(), activityPage.getTotal());
        List<ActivityEffectDetailVO> voList = activityPage.getRecords().stream()
                .map(this::buildActivityEffectDetailSummary).collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private ActivityEffectDetailVO buildActivityEffectDetailSummary(Activity activity) {
        ActivityEffectDetailVO vo = new ActivityEffectDetailVO();
        BeanUtils.copyProperties(activity, vo);
        ActivityEffectDetailVO.EffectSummary summary = buildEffectSummary(activity);
        vo.setEffectSummary(summary);
        return vo;
    }

    @Override
    public MiniPersonalBenefitVO getMiniPersonalBenefit(MiniBenefitQueryDTO dto) {
        if (dto.getMemberId() == null) {
            throw new BusinessException("会员ID不能为空");
        }
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        MiniPersonalBenefitVO result = new MiniPersonalBenefitVO();

        result.setMemberCard(buildMiniMemberCard(member));
        result.setPointInfo(buildMiniPointInfo(member));
        result.setBirthdayInfo(buildMiniBirthdayInfo(member));
        result.setLevelBenefits(buildLevelBenefits(member.getLevelCode()));
        result.setExpireReminders(buildExpireReminders(member.getId()));
        result.setCouponPage(buildMiniCouponPage(member.getId(), dto));
        result.setConsumeStats(buildMiniConsumeStats(member.getId()));

        return result;
    }

    private MiniMemberCardVO buildMiniMemberCard(Member member) {
        MiniMemberCardVO vo = new MiniMemberCardVO();
        vo.setMemberId(member.getId());
        vo.setMemberCode(member.getMemberCode());
        vo.setName(member.getName());
        vo.setPhone(member.getPhone());
        vo.setAvatar(member.getAvatar());

        LevelRule currentRule = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>()
                        .eq(LevelRule::getLevelCode, member.getLevelCode())
                        .eq(LevelRule::getStatus, 1)
                        .last("LIMIT 1")
        );

        if (currentRule != null) {
            vo.setLevelCode(currentRule.getLevelCode());
            vo.setLevelName(currentRule.getLevelName());
            vo.setLevelIcon(currentRule.getIcon());
            vo.setCurrentLevelBenefitDesc(currentRule.getBenefitDesc());
        } else {
            MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
            vo.setLevelCode(levelEnum.getCode());
            vo.setLevelName(levelEnum.getName());
            vo.setCurrentLevelBenefitDesc(levelEnum.getDesc());
        }

        vo.setGrowthValue(member.getGrowthValue() != null ? member.getGrowthValue() : 0);

        List<LevelRule> allRules = levelRuleMapper.selectList(
                new LambdaQueryWrapper<LevelRule>()
                        .eq(LevelRule::getStatus, 1)
                        .orderByAsc(LevelRule::getLevelCode)
        );

        Integer nextThreshold = null;
        String nextLevelBenefitDesc = null;
        if (!CollectionUtils.isEmpty(allRules)) {
            for (LevelRule rule : allRules) {
                if (rule.getLevelCode() > member.getLevelCode()) {
                    nextThreshold = rule.getGrowthThreshold();
                    nextLevelBenefitDesc = rule.getBenefitDesc();
                    break;
                }
            }
        }
        if (nextThreshold == null) {
            nextThreshold = vo.getGrowthValue();
        }
        vo.setNextLevelThreshold(nextThreshold);
        vo.setNextLevelBenefitDesc(nextLevelBenefitDesc);

        if (nextThreshold > 0 && vo.getGrowthValue() < nextThreshold) {
            BigDecimal progress = new BigDecimal(vo.getGrowthValue())
                    .multiply(PERCENT_100)
                    .divide(new BigDecimal(nextThreshold), 2, RoundingMode.HALF_UP);
            vo.setGrowthProgress(progress);
        } else {
            vo.setGrowthProgress(PERCENT_100);
        }

        Map<String, Object> totalConsume = queryStatsMapper.sumMemberConsume(member.getId(), null, null);
        if (totalConsume != null && totalConsume.get("totalAmount") != null) {
            vo.setTotalSpentAmount(new BigDecimal(totalConsume.get("totalAmount").toString()));
        } else {
            vo.setTotalSpentAmount(BigDecimal.ZERO);
        }

        return vo;
    }

    private MiniPointVO buildMiniPointInfo(Member member) {
        MiniPointVO vo = new MiniPointVO();
        vo.setCurrentPoints(member.getCurrentPoints() != null ? member.getCurrentPoints() : 0);

        List<PointLog> frozenLogs = pointLogMapper.selectList(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getMemberId, member.getId())
                        .eq(PointLog::getPointType, PointTypeEnum.FREEZE.getCode())
        );
        int frozenPoints = frozenLogs.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        vo.setFrozenPoints(frozenPoints);

        List<PointLog> expiring7 = pointLogMapper.selectExpiringPoints(member.getId(), 7);
        int expiringIn7Days = expiring7.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        vo.setExpiringIn7Days(expiringIn7Days);

        List<PointLog> expiring30 = pointLogMapper.selectExpiringPoints(member.getId(), 30);
        int expiringIn30Days = expiring30.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();
        vo.setExpiringIn30Days(expiringIn30Days);

        vo.setExpireSoonFlag(expiringIn30Days > 0);

        return vo;
    }

    private MiniBirthdayVO buildMiniBirthdayInfo(Member member) {
        MiniBirthdayVO vo = new MiniBirthdayVO();

        if (member.getBirthday() != null) {
            LocalDate birthday = member.getBirthday();
            MonthDay monthDay = MonthDay.from(birthday);
            vo.setBirthdayDate(monthDay.format(BIRTHDAY_FORMATTER));

            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            LocalDate thisYearBirthday = monthDay.atYear(currentYear);

            if (thisYearBirthday.isBefore(today)) {
                thisYearBirthday = monthDay.atYear(currentYear + 1);
            }
            int daysUntil = (int) ChronoUnit.DAYS.between(today, thisYearBirthday);
            vo.setDaysUntilBirthday(daysUntil);

            String birthdayKey = BIRTHDAY_KEY_PREFIX + currentYear + ":" + member.getId();
            Boolean alreadyGranted = stringRedisTemplate.hasKey(birthdayKey);

            LocalDate birthdayThisYear = monthDay.atYear(currentYear);
            if (today.isAfter(birthdayThisYear)) {
                vo.setThisYearBenefitStatus(2);
            } else if (Boolean.TRUE.equals(alreadyGranted)) {
                vo.setThisYearBenefitStatus(1);
            } else {
                vo.setThisYearBenefitStatus(0);
            }

            LevelRule rule = levelRuleMapper.selectOne(
                    new LambdaQueryWrapper<LevelRule>()
                            .eq(LevelRule::getLevelCode, member.getLevelCode())
                            .eq(LevelRule::getStatus, 1)
                            .last("LIMIT 1")
            );
            if (rule != null) {
                vo.setGrantedPoints(rule.getBirthdayPoints() != null ? rule.getBirthdayPoints() : 0);
                vo.setGrantedCouponCount(rule.getBirthdayCouponId() != null ? 1 : 0);
                StringBuilder preview = new StringBuilder();
                if (rule.getBirthdayPoints() != null && rule.getBirthdayPoints() > 0) {
                    preview.append("生日赠").append(rule.getBirthdayPoints()).append("积分");
                }
                if (rule.getBirthdayCouponId() != null) {
                    if (preview.length() > 0) {
                        preview.append("、");
                    }
                    preview.append("专属优惠券");
                }
                vo.setNextBirthdayBenefitPreview(preview.toString());
            } else {
                vo.setGrantedPoints(0);
                vo.setGrantedCouponCount(0);
                vo.setNextBirthdayBenefitPreview("");
            }
        } else {
            vo.setBirthdayDate("");
            vo.setDaysUntilBirthday(null);
            vo.setThisYearBenefitStatus(0);
            vo.setGrantedPoints(0);
            vo.setGrantedCouponCount(0);
            vo.setNextBirthdayBenefitPreview("");
        }

        return vo;
    }

    private List<LevelBenefitItemVO> buildLevelBenefits(Integer currentLevelCode) {
        List<LevelBenefitItemVO> result = new ArrayList<>();

        List<LevelRule> allRules = levelRuleMapper.selectList(
                new LambdaQueryWrapper<LevelRule>()
                        .eq(LevelRule::getStatus, 1)
                        .orderByAsc(LevelRule::getLevelCode)
        );

        if (CollectionUtils.isEmpty(allRules)) {
            for (MemberLevelEnum levelEnum : MemberLevelEnum.values()) {
                LevelBenefitItemVO item = new LevelBenefitItemVO();
                item.setTitle(levelEnum.getName() + "会员");
                item.setDesc(levelEnum.getDesc());
                item.setLevelRequired(levelEnum.getCode());
                item.setAchieved(currentLevelCode != null && currentLevelCode >= levelEnum.getCode());
                result.add(item);
            }
        } else {
            for (LevelRule rule : allRules) {
                LevelBenefitItemVO item = new LevelBenefitItemVO();
                item.setTitle(rule.getLevelName() + "会员");
                item.setDesc(rule.getBenefitDesc());
                item.setIcon(rule.getIcon());
                item.setLevelRequired(rule.getLevelCode());
                item.setAchieved(currentLevelCode != null && currentLevelCode >= rule.getLevelCode());
                result.add(item);
            }
        }

        return result;
    }

    private List<ExpireReminderVO> buildExpireReminders(Long memberId) {
        List<ExpireReminderVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in3Days = now.plus(3, ChronoUnit.DAYS);
        LocalDateTime in7Days = now.plus(7, ChronoUnit.DAYS);

        List<CouponInstance> expiringCoupons = couponInstanceMapper.selectList(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getMemberId, memberId)
                        .in(CouponInstance::getCouponStatus, Arrays.asList(
                                CouponStatusEnum.AVAILABLE.getCode(),
                                CouponStatusEnum.NOT_STARTED.getCode()
                        ))
                        .ge(CouponInstance::getValidEnd, now)
                        .le(CouponInstance::getValidEnd, in7Days)
        );

        if (!CollectionUtils.isEmpty(expiringCoupons)) {
            long in3Count = expiringCoupons.stream()
                    .filter(c -> c.getValidEnd() != null && c.getValidEnd().isBefore(in3Days))
                    .count();
            ExpireReminderVO couponReminder = new ExpireReminderVO();
            couponReminder.setType(1);
            couponReminder.setTitle("优惠券即将过期");
            if (in3Count > 0) {
                couponReminder.setContent("您有" + expiringCoupons.size() + "张优惠券将在7天内过期，其中" + in3Count + "张3天内过期");
            } else {
                couponReminder.setContent("您有" + expiringCoupons.size() + "张优惠券将在7天内过期");
            }
            couponReminder.setCount(expiringCoupons.size());
            couponReminder.setExpireDateRange("7天内");
            result.add(couponReminder);
        }

        List<PointLog> expiringPoints = pointLogMapper.selectExpiringPoints(memberId, 7);
        int expiringPointCount = expiringPoints.stream()
                .mapToInt(p -> p.getChangePoints() != null ? p.getChangePoints() : 0).sum();

        if (expiringPointCount > 0) {
            ExpireReminderVO pointReminder = new ExpireReminderVO();
            pointReminder.setType(2);
            pointReminder.setTitle("积分即将过期");
            pointReminder.setContent("您有" + expiringPointCount + "积分将在7天内过期，请尽快使用");
            pointReminder.setCount(expiringPointCount);
            pointReminder.setExpireDateRange("7天内");
            result.add(pointReminder);
        }

        return result;
    }

    private PageResult<MiniCouponVO> buildMiniCouponPage(Long memberId, MiniBenefitQueryDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> statuses;

        if (!CollectionUtils.isEmpty(dto.getCouponStatusFilter())) {
            statuses = dto.getCouponStatusFilter();
        } else {
            statuses = Boolean.TRUE.equals(dto.getIncludeExpired())
                    ? Arrays.asList(
                            CouponStatusEnum.AVAILABLE.getCode(),
                            CouponStatusEnum.NOT_STARTED.getCode(),
                            CouponStatusEnum.LOCKED.getCode(),
                            CouponStatusEnum.USED.getCode(),
                            CouponStatusEnum.EXPIRED.getCode(),
                            CouponStatusEnum.INACTIVE.getCode())
                    : Arrays.asList(
                            CouponStatusEnum.AVAILABLE.getCode(),
                            CouponStatusEnum.NOT_STARTED.getCode(),
                            CouponStatusEnum.LOCKED.getCode());
        }

        List<CouponInstance> allCoupons = couponInstanceMapper.selectList(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getMemberId, memberId)
                        .in(CouponInstance::getCouponStatus, statuses)
        );

        Map<Integer, Integer> statusPriority = new HashMap<>();
        statusPriority.put(CouponStatusEnum.AVAILABLE.getCode(), 1);
        statusPriority.put(CouponStatusEnum.NOT_STARTED.getCode(), 2);
        statusPriority.put(CouponStatusEnum.LOCKED.getCode(), 3);
        statusPriority.put(CouponStatusEnum.USED.getCode(), 4);
        statusPriority.put(CouponStatusEnum.EXPIRED.getCode(), 5);
        statusPriority.put(CouponStatusEnum.INACTIVE.getCode(), 6);

        allCoupons.sort(Comparator
                .comparingInt((CouponInstance c) -> statusPriority.getOrDefault(c.getCouponStatus(), 99))
                .thenComparing(CouponInstance::getValidEnd, Comparator.nullsLast(Comparator.naturalOrder())));

        long total = allCoupons.size();
        int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allCoupons.size());

        List<CouponInstance> pageCoupons = fromIndex >= total
                ? new ArrayList<>()
                : allCoupons.subList(fromIndex, toIndex);

        List<Long> templateIds = pageCoupons.stream()
                .map(CouponInstance::getTemplateId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, CouponTemplate> templateMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(templateIds)) {
            List<CouponTemplate> templates = couponTemplateMapper.selectBatchIds(templateIds);
            for (CouponTemplate t : templates) {
                templateMap.put(t.getId(), t);
            }
        }

        List<MiniCouponVO> voList = pageCoupons.stream().map(ci -> {
            MiniCouponVO vo = new MiniCouponVO();
            vo.setInstanceId(ci.getId());
            vo.setTemplateId(ci.getTemplateId());
            vo.setStatus(ci.getCouponStatus());
            vo.setStatusName(getCouponStatusName(ci.getCouponStatus()));
            vo.setValidStart(ci.getValidStart());
            vo.setValidEnd(ci.getValidEnd());

            if (ci.getValidEnd() != null) {
                long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), ci.getValidEnd().toLocalDate());
                vo.setDaysLeft((int) daysLeft);
                vo.setExpireTag(daysLeft <= 7 && daysLeft >= 0);
            } else {
                vo.setDaysLeft(null);
                vo.setExpireTag(false);
            }

            CouponTemplate template = templateMap.get(ci.getTemplateId());
            if (template != null) {
                vo.setCouponName(template.getCouponName());
                vo.setCouponTypeName(getCouponTypeName(template.getCouponType()));
                vo.setFullAmount(template.getFullAmount());
                vo.setReduceAmount(template.getReduceAmount());
                vo.setExchangeItem(template.getExchangeItem());
            }

            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, total, (long) pageNum, (long) pageSize);
    }

    private String getCouponStatusName(Integer status) {
        if (status == null) return "";
        for (CouponStatusEnum e : CouponStatusEnum.values()) {
            if (e.getCode().equals(status)) {
                return e.getName();
            }
        }
        return "";
    }

    private String getCouponTypeName(Integer type) {
        if (type == null) return "";
        for (CouponTypeEnum e : CouponTypeEnum.values()) {
            if (e.getCode().equals(type)) {
                return e.getName();
            }
        }
        return "";
    }

    private MiniConsumeStatsVO buildMiniConsumeStats(Long memberId) {
        MiniConsumeStatsVO vo = new MiniConsumeStatsVO();
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Long monthCount = queryStatsMapper.countMemberConsume(memberId, monthStart, monthEnd);
        vo.setMonthCount(monthCount != null ? monthCount.intValue() : 0);

        Map<String, Object> monthSum = queryStatsMapper.sumMemberConsume(memberId, monthStart, monthEnd);
        if (monthSum != null && monthSum.get("totalAmount") != null) {
            vo.setMonthAmount(new BigDecimal(monthSum.get("totalAmount").toString()));
        } else {
            vo.setMonthAmount(BigDecimal.ZERO);
        }

        Long totalCount = queryStatsMapper.countMemberConsume(memberId, null, null);
        vo.setTotalCount(totalCount != null ? totalCount.intValue() : 0);

        Map<String, Object> totalSum = queryStatsMapper.sumMemberConsume(memberId, null, null);
        if (totalSum != null && totalSum.get("totalAmount") != null) {
            vo.setTotalAmount(new BigDecimal(totalSum.get("totalAmount").toString()));
        } else {
            vo.setTotalAmount(BigDecimal.ZERO);
        }

        List<ConsumeOrder> lastOrders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .eq(ConsumeOrder::getMemberId, memberId)
                        .in(ConsumeOrder::getOrderStatus, Arrays.asList(1, 2))
                        .orderByDesc(ConsumeOrder::getPayTime)
                        .last("LIMIT 1")
        );
        if (!CollectionUtils.isEmpty(lastOrders)) {
            vo.setLastConsumeTime(lastOrders.get(0).getPayTime());
        }

        return vo;
    }
}
