package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "权益履约状态总览VO")
public class BenefitFulfillmentVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "订单状态名称")
    private String orderStatusName;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "实际支付金额")
    private BigDecimal payAmount;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "券优惠金额")
    private BigDecimal couponSavings;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointSavings;

    @Schema(description = "等级折扣金额")
    private BigDecimal levelSavings;

    @Schema(description = "总优惠金额")
    private BigDecimal totalSavings;

    @Schema(description = "履约明细步骤")
    private List<FulfillmentItemVO> items;

    @Schema(description = "客服处理信息")
    private FulfillmentCSVO customerService;

    @Schema(description = "风险记录")
    private List<FulfillmentRiskVO> risks;
}
