package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.service.GrayHitService;
import com.smartretail.mbc.query.dto.GrayEffectQueryDTO;
import com.smartretail.mbc.query.dto.GrayRuleCreateDTO;
import com.smartretail.mbc.query.entity.Activity;
import com.smartretail.mbc.query.entity.GrayMetric;
import com.smartretail.mbc.query.entity.GrayRule;
import com.smartretail.mbc.query.mapper.ActivityMapper;
import com.smartretail.mbc.query.mapper.CrowdMemberMapper;
import com.smartretail.mbc.query.mapper.GrayMetricMapper;
import com.smartretail.mbc.query.mapper.GrayRuleMapper;
import com.smartretail.mbc.query.service.GrayService;
import com.smartretail.mbc.query.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrayServiceImpl implements GrayService, GrayHitService {

    private final GrayRuleMapper grayRuleMapper;
    private final GrayMetricMapper grayMetricMapper;
    private final ActivityMapper activityMapper;
    private final CrowdMemberMapper crowdMemberMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int GRAY_STATUS_DRAFT = 0;
    private static final int GRAY_STATUS_GRAYING = 1;
    private static final int GRAY_STATUS_FULL = 2;
    private static final int GRAY_STATUS_ROLLBACK = 3;

    private static final int GRAY_TYPE_CITY = 1;
    private static final int GRAY_TYPE_STORE = 2;
    private static final int GRAY_TYPE_CROWD = 3;
    private static final int GRAY_TYPE_DEVICE = 4;

    private static final int GROUP_TYPE_GRAY = 1;
    private static final int GROUP_TYPE_CONTROL = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrayRuleVO createGrayRule(GrayRuleCreateDTO dto) {
        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayRule::getGrayCode, dto.getGrayCode());
        Long count = grayRuleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("灰度规则编码已存在");
        }

        Activity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException("关联活动不存在");
        }

        GrayRule grayRule = new GrayRule();
        grayRule.setGrayCode(dto.getGrayCode());
        grayRule.setGrayName(dto.getGrayName());
        grayRule.setActivityId(dto.getActivityId());
        grayRule.setGrayType(dto.getGrayType());
        grayRule.setGrayConfig(dto.getGrayConfig());
        grayRule.setRuleContent(dto.getRuleContent());
        grayRule.setOriginalRuleContent(activity.getRuleConfig());
        grayRule.setGrayRatio(dto.getGrayRatio() != null ? dto.getGrayRatio() : 10);
        grayRule.setStatus(GRAY_STATUS_DRAFT);
        grayRule.setOperator(dto.getOperator());
        grayRule.setDescription(dto.getDescription());
        grayRuleMapper.insert(grayRule);

        return convertToGrayRuleVO(grayRule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startGray(Long grayRuleId, String operator) {
        GrayRule grayRule = grayRuleMapper.selectById(grayRuleId);
        if (grayRule == null) {
            throw new BusinessException("灰度规则不存在");
        }
        if (!GRAY_STATUS_DRAFT.equals(grayRule.getStatus())) {
            throw new BusinessException("只有草稿状态的规则才能开始灰度");
        }

        grayRule.setStatus(GRAY_STATUS_GRAYING);
        grayRule.setStartGrayTime(LocalDateTime.now());
        grayRule.setOperator(operator);
        grayRuleMapper.updateById(grayRule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fullRelease(Long grayRuleId, String operator) {
        GrayRule grayRule = grayRuleMapper.selectById(grayRuleId);
        if (grayRule == null) {
            throw new BusinessException("灰度规则不存在");
        }
        if (!GRAY_STATUS_GRAYING.equals(grayRule.getStatus())) {
            throw new BusinessException("只有灰度中的规则才能全量发布");
        }

        Activity activity = activityMapper.selectById(grayRule.getActivityId());
        if (activity == null) {
            throw new BusinessException("关联活动不存在");
        }

        activity.setRuleConfig(grayRule.getRuleContent());
        activityMapper.updateById(activity);

        grayRule.setStatus(GRAY_STATUS_FULL);
        grayRule.setFullReleaseTime(LocalDateTime.now());
        grayRule.setOperator(operator);
        grayRuleMapper.updateById(grayRule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long grayRuleId, String operator) {
        GrayRule grayRule = grayRuleMapper.selectById(grayRuleId);
        if (grayRule == null) {
            throw new BusinessException("灰度规则不存在");
        }
        if (!GRAY_STATUS_GRAYING.equals(grayRule.getStatus()) && !GRAY_STATUS_FULL.equals(grayRule.getStatus())) {
            throw new BusinessException("只有灰度中或已全量的规则才能回滚");
        }

        Activity activity = activityMapper.selectById(grayRule.getActivityId());
        if (activity == null) {
            throw new BusinessException("关联活动不存在");
        }

        activity.setRuleConfig(grayRule.getOriginalRuleContent());
        activityMapper.updateById(activity);

        grayRule.setStatus(GRAY_STATUS_ROLLBACK);
        grayRule.setRollbackTime(LocalDateTime.now());
        grayRule.setOperator(operator);
        grayRuleMapper.updateById(grayRule);
    }

    @Override
    public GrayEffectVO getGrayEffect(GrayEffectQueryDTO dto) {
        GrayEffectVO effectVO = new GrayEffectVO();

        GrayMetric grayAgg = grayMetricMapper.selectAggregatedByGrayRuleIdAndGroupType(
                dto.getGrayRuleId(), GROUP_TYPE_GRAY, dto.getStartDate(), dto.getEndDate());
        GrayMetric controlAgg = grayMetricMapper.selectAggregatedByGrayRuleIdAndGroupType(
                dto.getGrayRuleId(), GROUP_TYPE_CONTROL, dto.getStartDate(), dto.getEndDate());

        effectVO.setGrayGroup(convertToMetricItemVO(grayAgg));
        effectVO.setControlGroup(convertToMetricItemVO(controlAgg));
        effectVO.setComparison(calculateComparison(grayAgg, controlAgg));

        List<GrayMetric> dailyMetrics = grayMetricMapper.selectByGrayRuleIdAndDateRange(
                dto.getGrayRuleId(), dto.getStartDate(), dto.getEndDate());
        effectVO.setDailyData(buildDailyData(dailyMetrics));

        return effectVO;
    }

    @Override
    public boolean checkGrayHit(Long activityId, Long memberId, String storeCode, String city, String posType) {
        if (activityId == null) {
            return false;
        }

        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayRule::getActivityId, activityId)
                .eq(GrayRule::getStatus, GRAY_STATUS_GRAYING)
                .orderByDesc(GrayRule::getCreateTime)
                .last("LIMIT 1");
        GrayRule grayRule = grayRuleMapper.selectOne(wrapper);

        if (grayRule == null) {
            return false;
        }

        boolean typeMatch = checkGrayTypeMatch(grayRule, memberId, storeCode, city, posType);
        if (!typeMatch) {
            return false;
        }

        Integer grayRatio = grayRule.getGrayRatio();
        if (grayRatio == null || grayRatio >= 100) {
            return true;
        }
        if (grayRatio <= 0) {
            return false;
        }

        if (memberId != null) {
            int hash = Math.abs(memberId.hashCode());
            int mod = hash % 100;
            return mod < grayRatio;
        }

        return false;
    }

    private boolean checkGrayTypeMatch(GrayRule grayRule, Long memberId, String storeCode, String city, String posType) {
        Integer grayType = grayRule.getGrayType();
        String grayConfig = grayRule.getGrayConfig();

        if (!StringUtils.hasText(grayConfig)) {
            return true;
        }

        Map<String, Object> configMap;
        try {
            configMap = objectMapper.readValue(grayConfig, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析灰度配置失败, grayRuleId: {}", grayRule.getId(), e);
            return false;
        }

        if (GRAY_TYPE_CITY.equals(grayType)) {
            List<String> cities = getConfigList(configMap, "cities");
            if (CollectionUtils.isEmpty(cities)) {
                return true;
            }
            return StringUtils.hasText(city) && cities.contains(city);
        }

        if (GRAY_TYPE_STORE.equals(grayType)) {
            List<String> storeCodes = getConfigList(configMap, "storeCodes");
            if (CollectionUtils.isEmpty(storeCodes)) {
                return true;
            }
            return StringUtils.hasText(storeCode) && storeCodes.contains(storeCode);
        }

        if (GRAY_TYPE_CROWD.equals(grayType)) {
            if (memberId == null) {
                return false;
            }
            List<Object> crowdIdsObj = getConfigList(configMap, "crowdIds");
            if (CollectionUtils.isEmpty(crowdIdsObj)) {
                return true;
            }
            List<Long> crowdIds = crowdIdsObj.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            for (Long crowdId : crowdIds) {
                List<Long> memberIds = crowdMemberMapper.selectMemberIdsByCrowdId(crowdId, 10000);
                if (memberIds != null && memberIds.contains(memberId)) {
                    return true;
                }
            }
            return false;
        }

        if (GRAY_TYPE_DEVICE.equals(grayType)) {
            List<String> posTypes = getConfigList(configMap, "posTypes");
            if (CollectionUtils.isEmpty(posTypes)) {
                return true;
            }
            return StringUtils.hasText(posType) && posTypes.contains(posType);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getConfigList(Map<String, Object> configMap, String key) {
        Object obj = configMap.get(key);
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        return Collections.emptyList();
    }

    @Override
    public GrayRuleVO getGrayRule(Long id) {
        GrayRule grayRule = grayRuleMapper.selectById(id);
        if (grayRule == null) {
            throw new BusinessException("灰度规则不存在");
        }
        return convertToGrayRuleVO(grayRule);
    }

    @Override
    public IPage<GrayRuleVO> listGrayRules(Long activityId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<>();
        if (activityId != null) {
            wrapper.eq(GrayRule::getActivityId, activityId);
        }
        if (status != null) {
            wrapper.eq(GrayRule::getStatus, status);
        }
        wrapper.orderByDesc(GrayRule::getCreateTime);

        Page<GrayRule> page = new Page<>(pageNum, pageSize);
        IPage<GrayRule> rulePage = grayRuleMapper.selectPage(page, wrapper);

        Page<GrayRuleVO> voPage = new Page<>(rulePage.getCurrent(), rulePage.getSize(), rulePage.getTotal());
        List<GrayRuleVO> voList = rulePage.getRecords().stream()
                .map(this::convertToGrayRuleVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordGrayMetric(Long grayRuleId, Integer groupType, Integer receiveCount,
                                 Integer redeemCount, BigDecimal redeemAmount,
                                 Integer orderCount, BigDecimal orderAmount) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<GrayMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayMetric::getGrayRuleId, grayRuleId)
                .eq(GrayMetric::getGroupType, groupType)
                .eq(GrayMetric::getStatDate, today);
        GrayMetric metric = grayMetricMapper.selectOne(wrapper);

        if (metric == null) {
            metric = new GrayMetric();
            metric.setGrayRuleId(grayRuleId);
            metric.setGroupType(groupType);
            metric.setStatDate(today);
            metric.setMemberCount(0);
            metric.setReceiveCount(receiveCount != null ? receiveCount : 0);
            metric.setRedeemCount(redeemCount != null ? redeemCount : 0);
            metric.setRedeemAmount(redeemAmount != null ? redeemAmount : BigDecimal.ZERO);
            metric.setOrderCount(orderCount != null ? orderCount : 0);
            metric.setOrderAmount(orderAmount != null ? orderAmount : BigDecimal.ZERO);
            metric.setRefundCount(0);
            metric.setRefundAmount(BigDecimal.ZERO);
            metric.setConversionRate(BigDecimal.ZERO);
            metric.setAvgOrderAmount(BigDecimal.ZERO);
            grayMetricMapper.insert(metric);
        } else {
            if (receiveCount != null && receiveCount > 0) {
                metric.setReceiveCount(metric.getReceiveCount() + receiveCount);
            }
            if (redeemCount != null && redeemCount > 0) {
                metric.setRedeemCount(metric.getRedeemCount() + redeemCount);
            }
            if (redeemAmount != null) {
                metric.setRedeemAmount(metric.getRedeemAmount().add(redeemAmount));
            }
            if (orderCount != null && orderCount > 0) {
                metric.setOrderCount(metric.getOrderCount() + orderCount);
            }
            if (orderAmount != null) {
                metric.setOrderAmount(metric.getOrderAmount().add(orderAmount));
            }
            grayMetricMapper.updateById(metric);
        }
    }

    private GrayRuleVO convertToGrayRuleVO(GrayRule grayRule) {
        GrayRuleVO vo = new GrayRuleVO();
        vo.setId(grayRule.getId());
        vo.setGrayCode(grayRule.getGrayCode());
        vo.setGrayName(grayRule.getGrayName());
        vo.setActivityId(grayRule.getActivityId());
        vo.setGrayType(grayRule.getGrayType());
        vo.setGrayConfig(grayRule.getGrayConfig());
        vo.setRuleContent(grayRule.getRuleContent());
        vo.setOriginalRuleContent(grayRule.getOriginalRuleContent());
        vo.setGrayRatio(grayRule.getGrayRatio());
        vo.setStatus(grayRule.getStatus());
        vo.setStartGrayTime(grayRule.getStartGrayTime());
        vo.setFullReleaseTime(grayRule.getFullReleaseTime());
        vo.setRollbackTime(grayRule.getRollbackTime());
        vo.setOperator(grayRule.getOperator());
        vo.setDescription(grayRule.getDescription());
        vo.setCreateTime(grayRule.getCreateTime());
        vo.setUpdateTime(grayRule.getUpdateTime());

        vo.setGrayTypeName(getGrayTypeName(grayRule.getGrayType()));
        vo.setStatusName(getStatusName(grayRule.getStatus()));

        return vo;
    }

    private String getGrayTypeName(Integer grayType) {
        if (grayType == null) {
            return "未知";
        }
        switch (grayType) {
            case GRAY_TYPE_CITY:
                return "城市灰度";
            case GRAY_TYPE_STORE:
                return "门店灰度";
            case GRAY_TYPE_CROWD:
                return "人群灰度";
            case GRAY_TYPE_DEVICE:
                return "设备灰度";
            default:
                return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case GRAY_STATUS_DRAFT:
                return "草稿";
            case GRAY_STATUS_GRAYING:
                return "灰度中";
            case GRAY_STATUS_FULL:
                return "已全量";
            case GRAY_STATUS_ROLLBACK:
                return "已回滚";
            default:
                return "未知";
        }
    }

    private GrayMetricItemVO convertToMetricItemVO(GrayMetric metric) {
        GrayMetricItemVO vo = new GrayMetricItemVO();
        if (metric == null) {
            vo.setMemberCount(0);
            vo.setReceiveCount(0);
            vo.setRedeemCount(0);
            vo.setRedeemAmount(BigDecimal.ZERO);
            vo.setOrderCount(0);
            vo.setOrderAmount(BigDecimal.ZERO);
            vo.setRefundCount(0);
            vo.setRefundAmount(BigDecimal.ZERO);
            vo.setConversionRate(BigDecimal.ZERO);
            vo.setAvgOrderAmount(BigDecimal.ZERO);
            vo.setRedeemRate(BigDecimal.ZERO);
            return vo;
        }

        vo.setMemberCount(metric.getMemberCount() != null ? metric.getMemberCount() : 0);
        vo.setReceiveCount(metric.getReceiveCount() != null ? metric.getReceiveCount() : 0);
        vo.setRedeemCount(metric.getRedeemCount() != null ? metric.getRedeemCount() : 0);
        vo.setRedeemAmount(metric.getRedeemAmount() != null ? metric.getRedeemAmount() : BigDecimal.ZERO);
        vo.setOrderCount(metric.getOrderCount() != null ? metric.getOrderCount() : 0);
        vo.setOrderAmount(metric.getOrderAmount() != null ? metric.getOrderAmount() : BigDecimal.ZERO);
        vo.setRefundCount(metric.getRefundCount() != null ? metric.getRefundCount() : 0);
        vo.setRefundAmount(metric.getRefundAmount() != null ? metric.getRefundAmount() : BigDecimal.ZERO);
        vo.setConversionRate(metric.getConversionRate() != null ? metric.getConversionRate() : BigDecimal.ZERO);
        vo.setAvgOrderAmount(metric.getAvgOrderAmount() != null ? metric.getAvgOrderAmount() : BigDecimal.ZERO);

        BigDecimal redeemRate = BigDecimal.ZERO;
        if (vo.getReceiveCount() != null && vo.getReceiveCount() > 0 && vo.getRedeemCount() != null) {
            redeemRate = BigDecimal.valueOf(vo.getRedeemCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(vo.getReceiveCount()), 2, RoundingMode.HALF_UP);
        }
        vo.setRedeemRate(redeemRate);

        return vo;
    }

    private GrayComparisonVO calculateComparison(GrayMetric gray, GrayMetric control) {
        GrayComparisonVO vo = new GrayComparisonVO();

        BigDecimal grayRedeemAmount = gray != null && gray.getRedeemAmount() != null
                ? gray.getRedeemAmount() : BigDecimal.ZERO;
        BigDecimal controlRedeemAmount = control != null && control.getRedeemAmount() != null
                ? control.getRedeemAmount() : BigDecimal.ZERO;
        BigDecimal grayOrderAmount = gray != null && gray.getOrderAmount() != null
                ? gray.getOrderAmount() : BigDecimal.ZERO;
        BigDecimal controlOrderAmount = control != null && control.getOrderAmount() != null
                ? control.getOrderAmount() : BigDecimal.ZERO;
        BigDecimal grayConversion = gray != null && gray.getConversionRate() != null
                ? gray.getConversionRate() : BigDecimal.ZERO;
        BigDecimal controlConversion = control != null && control.getConversionRate() != null
                ? control.getConversionRate() : BigDecimal.ZERO;

        vo.setRedeemAmountDiff(grayRedeemAmount.subtract(controlRedeemAmount));
        if (controlRedeemAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setRedeemAmountRatio(grayRedeemAmount.subtract(controlRedeemAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(controlRedeemAmount, 2, RoundingMode.HALF_UP));
        } else {
            vo.setRedeemAmountRatio(BigDecimal.ZERO);
        }

        vo.setOrderAmountDiff(grayOrderAmount.subtract(controlOrderAmount));
        if (controlOrderAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setOrderAmountRatio(grayOrderAmount.subtract(controlOrderAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(controlOrderAmount, 2, RoundingMode.HALF_UP));
        } else {
            vo.setOrderAmountRatio(BigDecimal.ZERO);
        }

        vo.setConversionDiff(grayConversion.subtract(controlConversion));
        vo.setRefundDiff(BigDecimal.ZERO);

        boolean isBetter = grayRedeemAmount.compareTo(controlRedeemAmount) > 0
                || (grayRedeemAmount.compareTo(controlRedeemAmount) == 0
                && grayConversion.compareTo(controlConversion) > 0);
        vo.setIsBetter(isBetter);

        return vo;
    }

    private List<GrayDailyItemVO> buildDailyData(List<GrayMetric> metrics) {
        if (CollectionUtils.isEmpty(metrics)) {
            return Collections.emptyList();
        }

        Map<LocalDate, GrayDailyItemVO> dailyMap = new TreeMap<>();

        for (GrayMetric metric : metrics) {
            LocalDate date = metric.getStatDate();
            if (date == null) continue;

            GrayDailyItemVO item = dailyMap.computeIfAbsent(date, k -> {
                GrayDailyItemVO vo = new GrayDailyItemVO();
                vo.setStatDate(k);
                vo.setGrayRedeemAmount(BigDecimal.ZERO);
                vo.setControlRedeemAmount(BigDecimal.ZERO);
                vo.setGrayConversionRate(BigDecimal.ZERO);
                vo.setControlConversionRate(BigDecimal.ZERO);
                return vo;
            });

            if (GROUP_TYPE_GRAY.equals(metric.getGroupType())) {
                item.setGrayRedeemAmount(metric.getRedeemAmount() != null ? metric.getRedeemAmount() : BigDecimal.ZERO);
                item.setGrayConversionRate(metric.getConversionRate() != null ? metric.getConversionRate() : BigDecimal.ZERO);
            } else if (GROUP_TYPE_CONTROL.equals(metric.getGroupType())) {
                item.setControlRedeemAmount(metric.getRedeemAmount() != null ? metric.getRedeemAmount() : BigDecimal.ZERO);
                item.setControlConversionRate(metric.getConversionRate() != null ? metric.getConversionRate() : BigDecimal.ZERO);
            }
        }

        return new ArrayList<>(dailyMap.values());
    }

    @Override
    public void recordGrayMetric(Long activityId, Long memberId, String storeCode, Integer groupType,
                                 Integer receiveCount, Integer redeemCount, BigDecimal redeemAmount,
                                 Integer orderCount, BigDecimal orderAmount) {
        if (activityId == null) {
            return;
        }

        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayRule::getActivityId, activityId)
                .eq(GrayRule::getStatus, GRAY_STATUS_GRAYING)
                .orderByDesc(GrayRule::getCreateTime)
                .last("LIMIT 1");
        GrayRule grayRule = grayRuleMapper.selectOne(wrapper);

        if (grayRule == null) {
            return;
        }

        recordGrayMetric(grayRule.getId(), groupType, receiveCount, redeemCount,
                redeemAmount, orderCount, orderAmount);
    }
}
