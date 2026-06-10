package com.smartretail.mbc.benefit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "权益返还请求（退款时）")
public class BenefitReturnDTO {

    @Schema(description = "锁定编号(与订单号二选一)")
    private String useNo;

    @Schema(description = "订单号(与锁定编号二选一)")
    private String orderNo;

    @Schema(description = "退款单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退款单号不能为空")
    private String refundNo;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "返还原因")
    private String returnReason;

    @Schema(description = "操作员")
    private String operator;
}
