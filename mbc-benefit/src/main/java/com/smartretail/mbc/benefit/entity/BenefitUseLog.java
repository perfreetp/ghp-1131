package com.smartretail.mbc.benefit.entity;

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
@TableName("t_benefit_use_log")
public class BenefitUseLog extends BaseEntity {

    private String useNo;

    private Long memberId;

    private Integer benefitType;

    private Long benefitId;

    private Integer useStatus;

    private String orderNo;

    private BigDecimal orderAmount;

    private BigDecimal benefitValue;

    private Integer usedPoints;

    private String storeCode;

    private String posCode;

    private String operator;

    private LocalDateTime lockTime;

    private LocalDateTime confirmTime;

    private LocalDateTime returnTime;

    private String returnReason;

    private String remark;
}
