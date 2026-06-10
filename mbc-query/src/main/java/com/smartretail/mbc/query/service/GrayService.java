package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.GrayActionDTO;
import com.smartretail.mbc.query.dto.GrayEffectQueryDTO;
import com.smartretail.mbc.query.dto.GrayRuleCreateDTO;
import com.smartretail.mbc.query.vo.GrayEffectVO;
import com.smartretail.mbc.query.vo.GrayRuleVO;

import java.util.List;

public interface GrayService {

    GrayRuleVO createGrayRule(GrayRuleCreateDTO dto);

    void startGray(Long grayRuleId, String operator);

    void fullRelease(Long grayRuleId, String operator);

    void rollback(Long grayRuleId, String operator);

    GrayEffectVO getGrayEffect(GrayEffectQueryDTO dto);

    boolean checkGrayHit(Long activityId, Long memberId, String storeCode, String city, String posType);

    GrayRuleVO getGrayRule(Long id);

    IPage<GrayRuleVO> listGrayRules(Long activityId, Integer status, Integer pageNum, Integer pageSize);

    void recordGrayMetric(Long grayRuleId, Integer groupType, Integer receiveCount, Integer redeemCount,
                          java.math.BigDecimal redeemAmount, Integer orderCount, java.math.BigDecimal orderAmount);
}
