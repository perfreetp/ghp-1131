package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.ActivityBudgetCreateDTO;
import com.smartretail.mbc.query.dto.ActivityBudgetQueryDTO;
import com.smartretail.mbc.query.entity.ActivityBudgetLog;
import com.smartretail.mbc.query.vo.ActivityBudgetProgressVO;

import java.math.BigDecimal;

public interface ActivityBudgetService {

    void createActivityBudget(ActivityBudgetCreateDTO dto);

    ActivityBudgetProgressVO getBudgetProgress(Long activityId);

    IPage<ActivityBudgetLog> getBudgetLogs(ActivityBudgetQueryDTO dto);

    boolean tryOccupyBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Long couponInstanceId);

    void confirmBudget(Long activityId, String storeCode, BigDecimal amount, String orderNo);

    void releaseBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Integer changeType);
}
