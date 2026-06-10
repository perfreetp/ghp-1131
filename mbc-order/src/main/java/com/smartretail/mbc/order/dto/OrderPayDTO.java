package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付完成请求")
public class OrderPayDTO {

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "实际支付金额")
    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;
}
