package com.smartretail.mbc.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "优惠券可用性结果VO")
public class CouponAvailabilityVO {

    @Schema(description = "是否可用")
    private Boolean available;

    @Schema(description = "不可用原因")
    private String unavailReason;

    @Schema(description = "减免金额")
    private BigDecimal reduceAmount;

    @Schema(description = "实际优惠价值")
    private BigDecimal actualValue;
}
