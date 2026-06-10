package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "退款请求")
public class OrderRefundDTO {

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "退款单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退款单号不能为空")
    private String refundNo;

    @Schema(description = "退款金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;
}
