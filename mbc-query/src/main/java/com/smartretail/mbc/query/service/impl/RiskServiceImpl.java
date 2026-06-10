package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.dto.RiskCheckDTO;
import com.smartretail.mbc.common.enums.RiskLevelEnum;
import com.smartretail.mbc.common.enums.RiskSceneEnum;
import com.smartretail.mbc.common.service.RiskCheckService;
import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.common.vo.RiskItemVO;
import com.smartretail.mbc.query.entity.RiskRecord;
import com.smartretail.mbc.query.entity.RiskRule;
import com.smartretail.mbc.query.mapper.RiskRecordMapper;
import com.smartretail.mbc.query.mapper.RiskRuleMapper;
import com.smartretail.mbc.query.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskCheckService riskCheckService;
    private final RiskRuleMapper riskRuleMapper;
    private final RiskRecordMapper riskRecordMapper;

    @Override
    public RiskCheckResultVO checkRisk(RiskCheckDTO dto) {
        RiskCheckResultVO result = riskCheckService.checkRisk(dto);

        List<RiskRule> enabledRules = riskRuleMapper.selectList(
                new LambdaQueryWrapper<RiskRule>()
                        .eq(RiskRule::getScene, dto.getScene())
                        .eq(RiskRule::getEnabled, true)
        );

        for (RiskRule rule : enabledRules) {
            if (matchesRule(result, rule)) {
                RiskItemVO item = new RiskItemVO();
                item.setRuleCode(rule.getRuleCode());
                item.setRuleName(rule.getRuleName());
                item.setCurrentValue(result.getRiskItems() != null && !result.getRiskItems().isEmpty()
                        ? result.getRiskItems().get(0).getCurrentValue() : "N/A");
                item.setThreshold(rule.getThresholdValue() != null ? rule.getThresholdValue().toPlainString() : "");
                item.setRiskLevel(rule.getRiskLevel());
                result.getRiskItems().add(item);

                if (rule.getRiskLevel() > result.getRiskLevel()) {
                    result.setRiskLevel(rule.getRiskLevel());
                    RiskLevelEnum levelEnum = RiskLevelEnum.getByCode(rule.getRiskLevel());
                    result.setRiskLevelName(levelEnum.getName());
                    updateAdvice(result, levelEnum);
                }
            }
        }

        if (result.getRiskLevel() >= RiskLevelEnum.MEDIUM.getCode()) {
            saveRiskRecord(dto, result);
        }

        return result;
    }

    @Override
    public IPage<RiskRecord> queryRiskRecords(Integer scene, Integer riskLevel, Integer handleResult,
                                               Integer pageNum, Integer pageSize) {
        Page<RiskRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        if (scene != null) {
            wrapper.eq(RiskRecord::getScene, scene);
        }
        if (riskLevel != null) {
            wrapper.eq(RiskRecord::getRiskLevel, riskLevel);
        }
        if (handleResult != null) {
            wrapper.eq(RiskRecord::getHandleResult, handleResult);
        }
        wrapper.orderByDesc(RiskRecord::getCreateTime);
        return riskRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public void handleRiskRecord(Long recordId, Integer handleResult, String handleStaff, String handleRemark) {
        RiskRecord record = riskRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("风控记录不存在: " + recordId);
        }
        record.setHandleResult(handleResult);
        record.setHandleStaff(handleStaff);
        record.setHandleTime(LocalDateTime.now());
        record.setHandleRemark(handleRemark);
        riskRecordMapper.updateById(record);
    }

    private boolean matchesRule(RiskCheckResultVO result, RiskRule rule) {
        if (result.getRiskItems() == null || result.getRiskItems().isEmpty()) {
            return false;
        }
        RiskSceneEnum sceneEnum = RiskSceneEnum.getByCode(rule.getScene());
        if (sceneEnum == null) {
            return false;
        }
        for (RiskItemVO item : result.getRiskItems()) {
            if (item.getRuleCode() != null && item.getRuleCode().startsWith("RISK_" + sceneEnum.name())) {
                try {
                    int currentVal = Integer.parseInt(item.getCurrentValue());
                    int threshold = rule.getThresholdValue() != null ? rule.getThresholdValue().intValue() : 0;
                    return currentVal > threshold;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private void updateAdvice(RiskCheckResultVO result, RiskLevelEnum levelEnum) {
        if (levelEnum == RiskLevelEnum.HIGH) {
            result.setAdvice("拦截");
            result.setPass(false);
        } else if (levelEnum == RiskLevelEnum.MEDIUM) {
            result.setAdvice("人工确认");
            result.setPass(false);
        } else {
            result.setAdvice("放行");
            result.setPass(true);
        }
    }

    private void saveRiskRecord(RiskCheckDTO dto, RiskCheckResultVO result) {
        for (RiskItemVO item : result.getRiskItems()) {
            RiskRecord record = new RiskRecord();
            record.setRecordNo("RR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            record.setScene(dto.getScene());
            record.setRiskLevel(item.getRiskLevel());
            record.setMemberId(dto.getMemberId());
            record.setStoreCode(dto.getStoreCode());
            record.setPosCode(dto.getPosCode());
            record.setOrderNo(dto.getOrderNo());
            record.setRuleCode(item.getRuleCode());
            record.setRuleName(item.getRuleName());
            record.setCurrentValue(item.getCurrentValue());
            record.setThresholdValue(item.getThreshold());
            record.setHandleResult(0);
            riskRecordMapper.insert(record);
        }
    }
}
