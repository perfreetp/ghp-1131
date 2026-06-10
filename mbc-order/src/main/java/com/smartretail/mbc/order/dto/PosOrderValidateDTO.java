package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "收银端订单试算请求")
public class PosOrderValidateDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "手机号（收银端可只传phone）")
    private String phone;

    @Schema(description = "会员码")
    private String memberCode;

    @Schema(description = "商品明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品明细不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @Schema(description = "订单总金额（自动算但也可传）")
    private BigDecimal totalAmount;

    @Schema(description = "想用的券ID列表")
    private List<Long> useCouponIds;

    @Schema(description = "自动尝试所有可用券并返回最优组合", defaultValue = "true")
    private Boolean tryAllCoupons = true;

    @Schema(description = "想使用的积分数，null表示自动算最大可用")
    private Integer usePoints;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "门店编码不能为空")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "收银员")
    private String cashier;

    @Schema(description = "渠道", defaultValue = "POS")
    private String channel = "POS";
}
