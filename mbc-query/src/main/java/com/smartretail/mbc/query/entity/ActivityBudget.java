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
@TableName("t_activity_budget")
public class ActivityBudget extends BaseEntity {

    private Long activityId;

    private String storeCode;

    private String storeName;

    private Integer budgetType;

    private BigDecimal totalBudget;

    private BigDecimal usedBudget;

    private Integer issueLimit;

    private Integer issuedCount;

    private Integer redeemLimit;

    private Integer redeemedCount;

    private Integer status;
}
