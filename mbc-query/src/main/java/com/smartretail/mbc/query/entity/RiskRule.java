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
@TableName("t_risk_rule")
public class RiskRule extends BaseEntity {

    private String ruleCode;

    private String ruleName;

    private Integer scene;

    private Integer conditionType;

    private Integer timeWindow;

    private BigDecimal thresholdValue;

    private Integer riskLevel;

    private Boolean enabled;
}
