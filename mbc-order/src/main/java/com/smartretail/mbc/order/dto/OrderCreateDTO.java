package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "创建订单请求")
public class OrderCreateDTO {

    @Schema(description = "订单号(幂等用)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "会员ID(可为null表示非会员)")
    private Long memberId;

    @Schema(description = "订单类型 默认1消费")
    private Integer orderType = 1;

    @Schema(description = "订单总金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单总金额不能为空")
    private BigDecimal totalAmount;

    @Schema(description = "使用积分 默认0")
    private Integer usedPoints = 0;

    @Schema(description = "使用的券实例ID列表")
    private List<Long> usedCouponInstanceIds;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "收银员")
    private String cashier;

    @Schema(description = "渠道")
    private String channel;
}
