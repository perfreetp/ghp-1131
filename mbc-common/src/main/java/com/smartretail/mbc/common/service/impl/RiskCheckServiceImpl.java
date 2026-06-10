package com.smartretail.mbc.common.service.impl;

import com.smartretail.mbc.common.dto.RiskCheckDTO;
import com.smartretail.mbc.common.enums.RiskLevelEnum;
import com.smartretail.mbc.common.enums.RiskSceneEnum;
import com.smartretail.mbc.common.service.RiskCheckService;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.common.vo.RiskItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskCheckServiceImpl implements RiskCheckService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int COUPON_WINDOW_SECONDS = 3600;
    private static final int POS_WINDOW_SECONDS = 3600;
    private static final int REFUND_WINDOW_SECONDS = 604800;
    private static final int CROSS_STORE_WINDOW_SECONDS = 604800;

    private static final int COUPON_LOW_THRESHOLD = 5;
    private static final int COUPON_MEDIUM_THRESHOLD = 10;
    private static final int COUPON_HIGH_THRESHOLD = 20;

    private static final int POS_LOW_THRESHOLD = 20;
    private static final int POS_MEDIUM_THRESHOLD = 50;
    private static final int POS_HIGH_THRESHOLD = 100;

    private static final int REFUND_LOW_THRESHOLD = 3;
    private static final int REFUND_MEDIUM_THRESHOLD = 5;
    private static final int REFUND_HIGH_THRESHOLD = 10;

    private static final int CROSS_STORE_LOW_THRESHOLD = 3;
    private static final int CROSS_STORE_MEDIUM_THRESHOLD = 5;
    private static final int CROSS_STORE_HIGH_THRESHOLD = 10;

    @Override
    public RiskCheckResultVO checkRisk(RiskCheckDTO dto) {
        RiskSceneEnum sceneEnum = RiskSceneEnum.getByCode(dto.getScene());
        if (sceneEnum == null) {
            return buildSafeResult(dto.getScene());
        }

        List<RiskItemVO> riskItems = new ArrayList<>();
        int windowSeconds;
        String identity;

        switch (sceneEnum) {
            case COUPON_RECEIVE:
                windowSeconds = COUPON_WINDOW_SECONDS;
                identity = String.valueOf(dto.getMemberId());
                checkByThresholds(dto.getScene(), sceneEnum, identity, windowSeconds,
                        COUPON_LOW_THRESHOLD, COUPON_MEDIUM_THRESHOLD, COUPON_HIGH_THRESHOLD,
                        "领券频次", riskItems);
                break;
            case POS_VALIDATE:
                windowSeconds = POS_WINDOW_SECONDS;
                identity = dto.getPosCode();
                checkByThresholds(dto.getScene(), sceneEnum, identity, windowSeconds,
                        POS_LOW_THRESHOLD, POS_MEDIUM_THRESHOLD, POS_HIGH_THRESHOLD,
                        "试算频次", riskItems);
                break;
            case REFUND_RETURN:
                windowSeconds = REFUND_WINDOW_SECONDS;
                identity = String.valueOf(dto.getMemberId());
                checkByThresholds(dto.getScene(), sceneEnum, identity, windowSeconds,
                        REFUND_LOW_THRESHOLD, REFUND_MEDIUM_THRESHOLD, REFUND_HIGH_THRESHOLD,
                        "退款频次", riskItems);
                break;
            case CROSS_STORE_REDEEM:
                windowSeconds = CROSS_STORE_WINDOW_SECONDS;
                identity = String.valueOf(dto.getMemberId());
                checkByThresholds(dto.getScene(), sceneEnum, identity, windowSeconds,
                        CROSS_STORE_LOW_THRESHOLD, CROSS_STORE_MEDIUM_THRESHOLD, CROSS_STORE_HIGH_THRESHOLD,
                        "跨店核销频次", riskItems);
                break;
            default:
                return buildSafeResult(dto.getScene());
        }

        return buildResult(dto.getScene(), sceneEnum, riskItems);
    }

    private void checkByThresholds(Integer scene, RiskSceneEnum sceneEnum, String identity,
                                   int windowSeconds, int lowThreshold, int mediumThreshold,
                                   int highThreshold, String ruleName, List<RiskItemVO> riskItems) {
        if (identity == null || identity.isEmpty()) {
            return;
        }

        String redisKey = RedisKeyUtil.riskCount(scene, identity);
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        int currentCount = count != null ? count.intValue() : 0;
        String ruleCodePrefix = "RISK_" + sceneEnum.name();

        if (currentCount > highThreshold) {
            riskItems.add(buildRiskItem(ruleCodePrefix + "_HIGH", ruleName + "-高风险",
                    String.valueOf(currentCount), String.valueOf(highThreshold), RiskLevelEnum.HIGH.getCode()));
        } else if (currentCount > mediumThreshold) {
            riskItems.add(buildRiskItem(ruleCodePrefix + "_MEDIUM", ruleName + "-中风险",
                    String.valueOf(currentCount), String.valueOf(mediumThreshold), RiskLevelEnum.MEDIUM.getCode()));
        } else if (currentCount > lowThreshold) {
            riskItems.add(buildRiskItem(ruleCodePrefix + "_LOW", ruleName + "-低风险",
                    String.valueOf(currentCount), String.valueOf(lowThreshold), RiskLevelEnum.LOW.getCode()));
        }
    }

    private RiskItemVO buildRiskItem(String ruleCode, String ruleName, String currentValue,
                                     String threshold, Integer riskLevel) {
        RiskItemVO item = new RiskItemVO();
        item.setRuleCode(ruleCode);
        item.setRuleName(ruleName);
        item.setCurrentValue(currentValue);
        item.setThreshold(threshold);
        item.setRiskLevel(riskLevel);
        return item;
    }

    private RiskCheckResultVO buildSafeResult(Integer scene) {
        RiskCheckResultVO result = new RiskCheckResultVO();
        result.setPass(true);
        result.setRiskLevel(RiskLevelEnum.SAFE.getCode());
        result.setRiskLevelName(RiskLevelEnum.SAFE.getName());
        result.setScene(scene);
        RiskSceneEnum sceneEnum = RiskSceneEnum.getByCode(scene);
        result.setSceneName(sceneEnum != null ? sceneEnum.getName() : "");
        result.setRiskItems(new ArrayList<>());
        result.setAdvice("放行");
        return result;
    }

    private RiskCheckResultVO buildResult(Integer scene, RiskSceneEnum sceneEnum, List<RiskItemVO> riskItems) {
        RiskCheckResultVO result = new RiskCheckResultVO();
        result.setScene(scene);
        result.setSceneName(sceneEnum.getName());
        result.setRiskItems(riskItems);

        if (riskItems.isEmpty()) {
            result.setPass(true);
            result.setRiskLevel(RiskLevelEnum.SAFE.getCode());
            result.setRiskLevelName(RiskLevelEnum.SAFE.getName());
            result.setAdvice("放行");
        } else {
            int maxLevel = riskItems.stream()
                    .mapToInt(RiskItemVO::getRiskLevel)
                    .max()
                    .orElse(RiskLevelEnum.SAFE.getCode());
            RiskLevelEnum levelEnum = RiskLevelEnum.getByCode(maxLevel);
            result.setRiskLevel(levelEnum.getCode());
            result.setRiskLevelName(levelEnum.getName());
            result.setPass(maxLevel < RiskLevelEnum.HIGH.getCode());
            if (maxLevel >= RiskLevelEnum.HIGH.getCode()) {
                result.setAdvice("拦截");
            } else if (maxLevel >= RiskLevelEnum.MEDIUM.getCode()) {
                result.setAdvice("人工确认");
            } else {
                result.setAdvice("放行");
            }
        }

        return result;
    }
}
