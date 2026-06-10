package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "灰度指标项VO")
public class GrayMetricItemVO {

    @Schema(description = "会员数")
    private Integer memberCount;

    @Schema(description = "领券数")
    private Integer receiveCount;

    @Schema(description = "核销数")
    private Integer redeemCount;

    @Schema(description = "核销金额")
    private BigDecimal redeemAmount;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "退款数")
    private Integer refundCount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "转化率%")
    private BigDecimal conversionRate;

    @Schema(description = "客单价")
    private BigDecimal avgOrderAmount;

    @Schema(description = "核销率%")
    private BigDecimal redeemRate;
}
