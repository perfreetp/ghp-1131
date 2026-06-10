package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单商品明细")
public class OrderItemDTO {

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "SKU ID不能为空")
    private String skuId;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    private Integer quantity;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;

    @Schema(description = "小计（单价×数量）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "小计不能为空")
    private BigDecimal subtotal;

    @Schema(description = "品类ID")
    private String categoryId;

    @Schema(description = "标记是否被排除商品")
    private Boolean isExcluded;
}
