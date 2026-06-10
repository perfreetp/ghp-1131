package com.smartretail.mbc.benefit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "确认核销请求")
public class BenefitConfirmDTO {

    @Schema(description = "锁定编号(可选，与订单号二选一)")
    private String useNo;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "实际订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "操作员")
    private String operator;
}
