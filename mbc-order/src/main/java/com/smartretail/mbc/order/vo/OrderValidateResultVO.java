package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "订单预校验结果VO")
public class OrderValidateResultVO {

    @Schema(description = "是否校验通过")
    private Boolean valid;

    @Schema(description = "校验不通过的原因列表")
    private List<String> invalidReasons;

    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponAmount;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointAmount;

    @Schema(description = "等级折扣金额")
    private BigDecimal levelDiscount;

    @Schema(description = "总优惠金额")
    private BigDecimal totalDiscount;

    @Schema(description = "最终支付金额")
    private BigDecimal finalPayAmount;

    @Schema(description = "可获得积分")
    private Integer earnablePoints;

    @Schema(description = "可获得成长值")
    private Integer earnableGrowth;

    @Schema(description = "应用的优惠券明细")
    private List<AppliedCouponVO> appliedCoupons;

    @Data
    @Schema(description = "应用的优惠券明细")
    public static class AppliedCouponVO {

        @Schema(description = "券实例ID")
        private Long couponInstanceId;

        @Schema(description = "券模板ID")
        private Long templateId;

        @Schema(description = "券名称")
        private String couponName;

        @Schema(description = "券类型")
        private Integer couponType;

        @Schema(description = "满减门槛")
        private BigDecimal fullAmount;

        @Schema(description = "减免金额")
        private BigDecimal reduceAmount;

        @Schema(description = "是否可叠加")
        private Integer stackable;
    }
}
