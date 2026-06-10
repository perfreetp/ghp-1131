package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_activity")
public class Activity extends BaseEntity {

    private String activityCode;

    private String activityName;

    private Integer activityType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer targetLevel;

    private Integer budgetPoints;

    private Integer budgetCoupons;

    private Integer usedPoints;

    private Integer usedCoupons;

    private Integer exposedCount;

    private Integer participatedCount;

    private Integer convertedCount;

    private BigDecimal drivenOrderAmount;

    private Integer drivenOrderCount;

    private Integer status;

    private String ruleConfig;

    private String description;
}
