package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_risk_record")
public class RiskRecord extends BaseEntity {

    private String recordNo;

    private Integer scene;

    private Integer riskLevel;

    private Long memberId;

    private String storeCode;

    private String posCode;

    private String orderNo;

    private String ruleCode;

    private String ruleName;

    private String currentValue;

    private String thresholdValue;

    private Integer handleResult;

    private String handleStaff;

    private LocalDateTime handleTime;

    private String handleRemark;
}
