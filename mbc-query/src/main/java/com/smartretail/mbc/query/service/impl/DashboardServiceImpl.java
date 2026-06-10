package com.smartretail.mbc.query.service.impl;

import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.query.dto.DashboardQueryDTO;
import com.smartretail.mbc.query.mapper.DashboardMapper;
import com.smartretail.mbc.query.service.DashboardService;
import com.smartretail.mbc.query.vo.AbnormalStoreVO;
import com.smartretail.mbc.query.vo.ActivityDashboardItemVO;
import com.smartretail.mbc.query.vo.DailyDashboardItemVO;
import com.smartretail.mbc.query.vo.DashboardSummaryVO;
import com.smartretail.mbc.query.vo.LevelDashboardItemVO;
import com.smartretail.mbc.query.vo.OperationDashboardVO;
import com.smartretail.mbc.query.vo.StoreDashboardItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    @Override
    public OperationDashboardVO getOperationDashboard(DashboardQueryDTO dto) {
        OperationDashboardVO result = new OperationDashboardVO();

        result.setSummary(buildSummary(dto.getStartDate(), dto.getEndDate()));
        result.setStoreRank(buildStoreRank(dto));
        result.setActivityRank(buildActivityRank(dto));
        result.setLevelDistribution(buildLevelDistribution(dto));
        result.setAbnormalStores(detectAbnormalStores());
        result.setRecentTrend(buildRecentTrend(dto));

        return result;
    }

    @Override
    public List<AbnormalStoreVO> getAbnormalStores(DashboardQueryDTO dto) {
        return detectAbnormalStores();
    }

    @Override
    public List<DailyDashboardItemVO> getTrendData(DashboardQueryDTO dto) {
        return buildRecentTrend(dto);
    }

    private DashboardSummaryVO buildSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summaryMap = dashboardMapper.selectDashboardSummary(startDate, endDate);
        DashboardSummaryVO summary = new DashboardSummaryVO();
        summary.setTotalCouponCount(getIntegerValue(summaryMap, "totalCouponCount"));
        summary.setTotalRedeemCount(getIntegerValue(summaryMap, "totalRedeemCount"));
        summary.setTotalRedeemAmount(getBigDecimalValue(summaryMap, "totalRedeemAmount"));
        summary.setTotalRefundCount(getIntegerValue(summaryMap, "totalRefundCount"));
        summary.setTotalRefundAmount(getBigDecimalValue(summaryMap, "totalRefundAmount"));
        summary.setTotalRiskCount(getIntegerValue(summaryMap, "totalRiskCount"));
        summary.setTotalBudgetUsed(getBigDecimalValue(summaryMap, "totalBudgetUsed"));
        summary.setTotalBudgetRemain(getBigDecimalValue(summaryMap, "totalBudgetRemain"));
        summary.setBudgetUsageRate(getBigDecimalValue(summaryMap, "budgetUsageRate"));
        summary.setRedeemRate(getBigDecimalValue(summaryMap, "redeemRate"));
        summary.setRefundRate(getBigDecimalValue(summaryMap, "refundRate"));
        return summary;
    }

    private List<StoreDashboardItemVO> buildStoreRank(DashboardQueryDTO dto) {
        List<Map<String, Object>> storeList = dashboardMapper.selectStoreDashboard(
                dto.getStartDate(), dto.getEndDate(),
                dto.getStoreCode(), dto.getCity(), dto.getProvince());

        if (CollectionUtils.isEmpty(storeList)) {
            return new ArrayList<>();
        }

        Map<String, Map<String, Object>> yesterdayMap = new HashMap<>();
        List<Map<String, Object>> yesterdayStats = dashboardMapper.selectYesterdayStoreStats();
        if (!CollectionUtils.isEmpty(yesterdayStats)) {
            yesterdayMap = yesterdayStats.stream()
                    .collect(Collectors.toMap(m -> (String) m.get("storeCode"), m -> m));
        }

        int rankTop = dto.getRankTop() != null ? dto.getRankTop() : 20;
        List<StoreDashboardItemVO> result = new ArrayList<>();

        for (int i = 0; i < Math.min(storeList.size(), rankTop); i++) {
            Map<String, Object> item = storeList.get(i);
            StoreDashboardItemVO vo = new StoreDashboardItemVO();
            vo.setStoreCode((String) item.get("storeCode"));
            vo.setStoreName((String) item.get("storeName"));
            vo.setStoreLevel(getIntegerValue(item, "storeLevel"));
            vo.setCity((String) item.get("city"));
            vo.setProvince((String) item.get("province"));
            vo.setCouponCount(getIntegerValue(item, "couponCount"));
            vo.setRedeemCount(getIntegerValue(item, "redeemCount"));
            vo.setRedeemAmount(getBigDecimalValue(item, "redeemAmount"));
            vo.setRefundCount(getIntegerValue(item, "refundCount"));
            vo.setRefundAmount(getBigDecimalValue(item, "refundAmount"));
            vo.setRiskCount(getIntegerValue(item, "riskCount"));
            vo.setBudgetUsed(getBigDecimalValue(item, "budgetUsed"));
            vo.setBudgetTotal(getBigDecimalValue(item, "budgetTotal"));
            vo.setBudgetUsageRate(getBigDecimalValue(item, "budgetUsageRate"));
            vo.setRedeemRate(getBigDecimalValue(item, "redeemRate"));
            vo.setRank(i + 1);

            Map<String, Object> yesterdayItem = yesterdayMap.get(vo.getStoreCode());
            if (yesterdayItem != null) {
                BigDecimal yesterdayRedeemAmount = getBigDecimalValue(yesterdayItem, "redeemCount");
                BigDecimal currentRedeemAmount = BigDecimal.valueOf(vo.getRedeemCount());
                if (yesterdayRedeemAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal changeRate = currentRedeemAmount.subtract(yesterdayRedeemAmount)
                            .divide(yesterdayRedeemAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                    vo.setChangeRate(changeRate);
                    if (changeRate.compareTo(BigDecimal.ZERO) > 0) {
                        vo.setTrend(1);
                    } else if (changeRate.compareTo(BigDecimal.ZERO) < 0) {
                        vo.setTrend(-1);
                    } else {
                        vo.setTrend(0);
                    }
                } else if (currentRedeemAmount.compareTo(BigDecimal.ZERO) > 0) {
                    vo.setTrend(1);
                    vo.setChangeRate(BigDecimal.valueOf(100));
                } else {
                    vo.setTrend(0);
                    vo.setChangeRate(BigDecimal.ZERO);
                }
            } else {
                vo.setTrend(0);
                vo.setChangeRate(BigDecimal.ZERO);
            }

            result.add(vo);
        }

        return result;
    }

    private List<ActivityDashboardItemVO> buildActivityRank(DashboardQueryDTO dto) {
        List<Map<String, Object>> activityList = dashboardMapper.selectActivityDashboard(
                dto.getStartDate(), dto.getEndDate(), dto.getActivityId());

        if (CollectionUtils.isEmpty(activityList)) {
            return new ArrayList<>();
        }

        int rankTop = dto.getRankTop() != null ? dto.getRankTop() : 20;
        List<ActivityDashboardItemVO> result = new ArrayList<>();

        for (int i = 0; i < Math.min(activityList.size(), rankTop); i++) {
            Map<String, Object> item = activityList.get(i);
            ActivityDashboardItemVO vo = new ActivityDashboardItemVO();
            vo.setActivityId(getLongValue(item, "activityId"));
            vo.setActivityCode((String) item.get("activityCode"));
            vo.setActivityName((String) item.get("activityName"));
            vo.setActivityType(getIntegerValue(item, "activityType"));
            vo.setCouponCount(getIntegerValue(item, "couponCount"));
            vo.setRedeemCount(getIntegerValue(item, "redeemCount"));
            vo.setRedeemAmount(getBigDecimalValue(item, "redeemAmount"));
            vo.setDrivenOrderCount(getIntegerValue(item, "drivenOrderCount"));
            vo.setDrivenOrderAmount(getBigDecimalValue(item, "drivenOrderAmount"));
            vo.setBudgetUsed(getBigDecimalValue(item, "budgetUsed"));
            vo.setBudgetTotal(getBigDecimalValue(item, "budgetTotal"));
            vo.setBudgetUsageRate(getBigDecimalValue(item, "budgetUsageRate"));
            vo.setStatus(getIntegerValue(item, "status"));
            vo.setDaysLeft(getIntegerValue(item, "daysLeft"));
            result.add(vo);
        }

        return result;
    }

    private List<LevelDashboardItemVO> buildLevelDistribution(DashboardQueryDTO dto) {
        List<Map<String, Object>> levelList = dashboardMapper.selectLevelDashboard(
                dto.getStartDate(), dto.getEndDate(), dto.getLevelCode());

        if (CollectionUtils.isEmpty(levelList)) {
            return new ArrayList<>();
        }

        List<LevelDashboardItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : levelList) {
            LevelDashboardItemVO vo = new LevelDashboardItemVO();
            Integer levelCode = getIntegerValue(item, "levelCode");
            vo.setLevelCode(levelCode);
            vo.setLevelName(MemberLevelEnum.getByCode(levelCode).getName());
            vo.setMemberCount(getIntegerValue(item, "memberCount"));
            vo.setCouponCount(getIntegerValue(item, "couponCount"));
            vo.setRedeemCount(getIntegerValue(item, "redeemCount"));
            vo.setRedeemAmount(getBigDecimalValue(item, "redeemAmount"));
            vo.setAvgCouponPerMember(getBigDecimalValue(item, "avgCouponPerMember"));
            result.add(vo);
        }

        return result;
    }

    private List<AbnormalStoreVO> detectAbnormalStores() {
        List<AbnormalStoreVO> abnormalStores = new ArrayList<>();
        LocalDateTime detectedTime = LocalDateTime.now();

        List<Map<String, Object>> todayStats = dashboardMapper.selectTodayStoreStats();
        List<Map<String, Object>> yesterdayStats = dashboardMapper.selectYesterdayStoreStats();

        Map<String, Map<String, Object>> yesterdayMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(yesterdayStats)) {
            yesterdayMap = yesterdayStats.stream()
                    .collect(Collectors.toMap(m -> (String) m.get("storeCode"), m -> m));
        }

        if (CollectionUtils.isEmpty(todayStats)) {
            return abnormalStores;
        }

        for (Map<String, Object> todayItem : todayStats) {
            String storeCode = (String) todayItem.get("storeCode");
            String storeName = (String) todayItem.get("storeName");
            String city = (String) todayItem.get("city");

            Map<String, Object> yesterdayItem = yesterdayMap.get(storeCode);

            int todayRedeem = getIntegerValue(todayItem, "redeemCount");
            int yesterdayRedeem = yesterdayItem != null ? getIntegerValue(yesterdayItem, "redeemCount") : 0;

            if (yesterdayRedeem > 0 && todayRedeem > yesterdayRedeem * 1.5) {
                AbnormalStoreVO vo = new AbnormalStoreVO();
                vo.setStoreCode(storeCode);
                vo.setStoreName(storeName);
                vo.setCity(city);
                vo.setAbnormalType(1);
                vo.setAbnormalTypeName("核销突增");
                vo.setCurrentValue(String.valueOf(todayRedeem));
                vo.setLastValue(String.valueOf(yesterdayRedeem));
                BigDecimal changeRate = BigDecimal.valueOf(todayRedeem - yesterdayRedeem)
                        .divide(BigDecimal.valueOf(yesterdayRedeem), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setChangeRate(changeRate);
                vo.setSeverity(determineSeverity(changeRate, 1));
                vo.setDetectedTime(detectedTime);
                abnormalStores.add(vo);
            }

            int todayRefund = getIntegerValue(todayItem, "refundCount");
            int yesterdayRefund = yesterdayItem != null ? getIntegerValue(yesterdayItem, "refundCount") : 0;

            if (yesterdayRefund > 0 && todayRefund > yesterdayRefund * 2) {
                AbnormalStoreVO vo = new AbnormalStoreVO();
                vo.setStoreCode(storeCode);
                vo.setStoreName(storeName);
                vo.setCity(city);
                vo.setAbnormalType(2);
                vo.setAbnormalTypeName("退款突增");
                vo.setCurrentValue(String.valueOf(todayRefund));
                vo.setLastValue(String.valueOf(yesterdayRefund));
                BigDecimal changeRate = BigDecimal.valueOf(todayRefund - yesterdayRefund)
                        .divide(BigDecimal.valueOf(yesterdayRefund), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setChangeRate(changeRate);
                vo.setSeverity(determineSeverity(changeRate, 2));
                vo.setDetectedTime(detectedTime);
                abnormalStores.add(vo);
            }

            int todayRisk = getIntegerValue(todayItem, "riskCount");
            int yesterdayRisk = yesterdayItem != null ? getIntegerValue(yesterdayItem, "riskCount") : 0;

            if (yesterdayRisk > 0 && todayRisk > yesterdayRisk * 2) {
                AbnormalStoreVO vo = new AbnormalStoreVO();
                vo.setStoreCode(storeCode);
                vo.setStoreName(storeName);
                vo.setCity(city);
                vo.setAbnormalType(3);
                vo.setAbnormalTypeName("风险突增");
                vo.setCurrentValue(String.valueOf(todayRisk));
                vo.setLastValue(String.valueOf(yesterdayRisk));
                BigDecimal changeRate = BigDecimal.valueOf(todayRisk - yesterdayRisk)
                        .divide(BigDecimal.valueOf(yesterdayRisk), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setChangeRate(changeRate);
                vo.setSeverity(determineSeverity(changeRate, 3));
                vo.setDetectedTime(detectedTime);
                abnormalStores.add(vo);
            }

            BigDecimal budgetUsageRate = getBigDecimalValue(todayItem, "budgetUsageRate");
            if (budgetUsageRate.compareTo(BigDecimal.valueOf(90)) >= 0) {
                AbnormalStoreVO vo = new AbnormalStoreVO();
                vo.setStoreCode(storeCode);
                vo.setStoreName(storeName);
                vo.setCity(city);
                vo.setAbnormalType(4);
                vo.setAbnormalTypeName("预算超支");
                vo.setCurrentValue(budgetUsageRate + "%");
                vo.setLastValue("90%");
                vo.setChangeRate(budgetUsageRate.subtract(BigDecimal.valueOf(90)));
                vo.setSeverity(determineSeverity(budgetUsageRate, 4));
                vo.setDetectedTime(detectedTime);
                abnormalStores.add(vo);
            }

            BigDecimal todayRedeemRate = getBigDecimalValue(todayItem, "redeemRate");
            BigDecimal yesterdayRedeemRate = yesterdayItem != null
                    ? getBigDecimalValue(yesterdayItem, "redeemRate")
                    : BigDecimal.ZERO;

            if (yesterdayRedeemRate.compareTo(BigDecimal.ZERO) > 0
                    && todayRedeemRate.compareTo(yesterdayRedeemRate.multiply(BigDecimal.valueOf(0.7))) < 0) {
                AbnormalStoreVO vo = new AbnormalStoreVO();
                vo.setStoreCode(storeCode);
                vo.setStoreName(storeName);
                vo.setCity(city);
                vo.setAbnormalType(5);
                vo.setAbnormalTypeName("核销率下降");
                vo.setCurrentValue(todayRedeemRate + "%");
                vo.setLastValue(yesterdayRedeemRate + "%");
                BigDecimal changeRate = todayRedeemRate.subtract(yesterdayRedeemRate)
                        .divide(yesterdayRedeemRate, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setChangeRate(changeRate);
                vo.setSeverity(determineSeverity(changeRate.abs(), 5));
                vo.setDetectedTime(detectedTime);
                abnormalStores.add(vo);
            }
        }

        abnormalStores.sort(Comparator.comparingInt(AbnormalStoreVO::getSeverity).reversed());

        return abnormalStores;
    }

    private int determineSeverity(BigDecimal value, int abnormalType) {
        switch (abnormalType) {
            case 1:
                if (value.compareTo(BigDecimal.valueOf(200)) >= 0) return 3;
                if (value.compareTo(BigDecimal.valueOf(100)) >= 0) return 2;
                return 1;
            case 2:
                if (value.compareTo(BigDecimal.valueOf(300)) >= 0) return 3;
                if (value.compareTo(BigDecimal.valueOf(150)) >= 0) return 2;
                return 1;
            case 3:
                if (value.compareTo(BigDecimal.valueOf(300)) >= 0) return 3;
                if (value.compareTo(BigDecimal.valueOf(150)) >= 0) return 2;
                return 1;
            case 4:
                if (value.compareTo(BigDecimal.valueOf(100)) >= 0) return 3;
                if (value.compareTo(BigDecimal.valueOf(95)) >= 0) return 2;
                return 1;
            case 5:
                if (value.compareTo(BigDecimal.valueOf(50)) >= 0) return 3;
                if (value.compareTo(BigDecimal.valueOf(30)) >= 0) return 2;
                return 1;
            default:
                return 1;
        }
    }

    private List<DailyDashboardItemVO> buildRecentTrend(DashboardQueryDTO dto) {
        int trendDays = dto.getTrendDays() != null ? dto.getTrendDays() : 7;
        LocalDate endDate = dto.getEndDate();
        LocalDate startDate = endDate.minusDays(trendDays - 1);

        List<Map<String, Object>> dailyList = dashboardMapper.selectDailyDashboard(startDate, endDate);

        if (CollectionUtils.isEmpty(dailyList)) {
            return new ArrayList<>();
        }

        List<DailyDashboardItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : dailyList) {
            DailyDashboardItemVO vo = new DailyDashboardItemVO();
            vo.setStatDate(getLocalDateValue(item, "statDate"));
            vo.setCouponCount(getIntegerValue(item, "couponCount"));
            vo.setRedeemCount(getIntegerValue(item, "redeemCount"));
            vo.setRedeemAmount(getBigDecimalValue(item, "redeemAmount"));
            vo.setRefundCount(getIntegerValue(item, "refundCount"));
            vo.setRefundAmount(getBigDecimalValue(item, "refundAmount"));
            vo.setRiskCount(getIntegerValue(item, "riskCount"));
            vo.setBudgetUsed(getBigDecimalValue(item, "budgetUsed"));
            result.add(vo);
        }

        return result;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private LocalDate getLocalDateValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
