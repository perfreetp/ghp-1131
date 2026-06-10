package com.smartretail.mbc.level.service;

import com.smartretail.mbc.level.dto.BirthdayBenefitDTO;
import com.smartretail.mbc.level.dto.GrowthCalcDTO;
import com.smartretail.mbc.level.dto.LevelAdjustDTO;
import com.smartretail.mbc.level.dto.LevelRuleUpsertDTO;
import com.smartretail.mbc.level.vo.BirthdayBenefitResultVO;
import com.smartretail.mbc.level.vo.GrowthResultVO;
import com.smartretail.mbc.level.vo.LevelRuleVO;

import java.util.List;

public interface LevelService {

    List<LevelRuleVO> listAllRules();

    LevelRuleVO getRuleByCode(Integer levelCode);

    void upsertRule(LevelRuleUpsertDTO dto);

    GrowthResultVO calcAndAddGrowth(GrowthCalcDTO dto);

    GrowthResultVO adjustLevel(LevelAdjustDTO dto);

    LevelRuleVO getCurrentLevel(Long memberId);

    boolean isLevelUp(Integer beforeCode, Integer afterCode);

    BirthdayBenefitResultVO grantBirthdayBenefit(BirthdayBenefitDTO dto);

    void processLevelChangeAsync(Long memberId, Integer beforeLevel, Integer afterLevel);
}
