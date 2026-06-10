package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "订单预校验请求")
public class OrderValidateDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "订单总金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单总金额不能为空")
    private BigDecimal totalAmount;

    @Schema(description = "使用的券ID列表")
    private List<Long> usedCouponIds;

    @Schema(description = "使用积分")
    private Integer usedPoints;

    @Schema(description = "门店编码")
    private String storeCode;
}
