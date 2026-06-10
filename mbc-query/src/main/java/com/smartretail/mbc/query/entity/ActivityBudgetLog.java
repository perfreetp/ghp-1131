package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_activity_budget_log")
public class ActivityBudgetLog extends BaseEntity {

    private Long activityId;

    private String storeCode;

    private Integer changeType;

    private BigDecimal changeAmount;

    private Integer changeQuantity;

    private BigDecimal beforeBudget;

    private BigDecimal afterBudget;

    private Integer beforeIssued;

    private Integer afterIssued;

    private Integer beforeRedeemed;

    private Integer afterRedeemed;

    private String orderNo;

    private Long couponInstanceId;
}
