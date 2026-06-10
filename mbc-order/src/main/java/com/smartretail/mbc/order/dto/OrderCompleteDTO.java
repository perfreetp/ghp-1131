package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "订单完成请求")
public class OrderCompleteDTO {

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}
