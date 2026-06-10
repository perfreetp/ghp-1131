package com.smartretail.mbc.common.service;

import java.math.BigDecimal;

public interface GrayHitService {

    boolean checkGrayHit(Long activityId, Long memberId, String storeCode, String city, String posType);

    void recordGrayMetric(Long activityId, Long memberId, String storeCode, Integer groupType,
                          Integer receiveCount, Integer redeemCount, BigDecimal redeemAmount,
                          Integer orderCount, BigDecimal orderAmount);
}
