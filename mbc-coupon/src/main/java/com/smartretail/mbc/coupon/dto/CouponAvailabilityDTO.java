package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "优惠券可用性判断请求")
public class CouponAvailabilityDTO {

    @NotNull(message = "券实例ID不能为空")
    @Schema(description = "券实例ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long instanceId;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "商品ID集合(用于排除校验)")
    private List<Long> itemIds;

    @Schema(description = "门店编码")
    private String storeCode;
}
