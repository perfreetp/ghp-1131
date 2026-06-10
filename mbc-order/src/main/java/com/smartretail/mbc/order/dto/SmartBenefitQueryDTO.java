package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "智能权益推荐查询")
public class SmartBenefitQueryDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @NotNull(message = "门店编码不能为空")
    @Schema(description = "当前定位门店编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeCode;

    @Schema(description = "POS设备类型编码，默认4(小程序收银)")
    private Integer posCode;

    @Schema(description = "购物车商品列表")
    private List<CartItemDTO> items;

    @Schema(description = "最多使用积分，不传则自动计算")
    private Integer maxPointsToUse;

    @Data
    @Schema(description = "购物车商品")
    public static class CartItemDTO {

        @Schema(description = "SKU ID")
        private String skuId;

        @Schema(description = "SKU名称")
        private String skuName;

        @Schema(description = "分类ID")
        private String categoryId;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "单价")
        private BigDecimal unitPrice;

        @Schema(description = "小计金额")
        private BigDecimal subtotal;
    }
}
