package com.smartretail.mbc.common.service;

import java.math.BigDecimal;

public interface BudgetOccupyService {

    boolean tryOccupyBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Long couponInstanceId);

    void confirmBudget(Long activityId, String storeCode, BigDecimal amount, String orderNo);

    void releaseBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Integer changeType);
}
