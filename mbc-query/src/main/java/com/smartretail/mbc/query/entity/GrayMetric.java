package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_gray_metric")
public class GrayMetric extends BaseEntity {

    private Long grayRuleId;

    private Integer groupType;

    private LocalDate statDate;

    private Integer memberCount;

    private Integer receiveCount;

    private Integer redeemCount;

    private BigDecimal redeemAmount;

    private Integer orderCount;

    private BigDecimal orderAmount;

    private Integer refundCount;

    private BigDecimal refundAmount;

    private BigDecimal conversionRate;

    private BigDecimal avgOrderAmount;
}
