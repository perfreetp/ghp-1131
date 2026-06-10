package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单统计VO")
public class OrderStatisticsVO {

    @Schema(description = "总订单数")
    private Long totalOrders;

    @Schema(description = "总支付金额")
    private BigDecimal totalPayAmount;

    @Schema(description = "总优惠金额")
    private BigDecimal totalDiscount;

    @Schema(description = "会员订单数")
    private Long memberOrders;

    @Schema(description = "会员支付金额占比")
    private BigDecimal memberPayRatio;
}
