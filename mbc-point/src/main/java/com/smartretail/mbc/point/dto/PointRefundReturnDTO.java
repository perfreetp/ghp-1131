package com.smartretail.mbc.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "退款积分返还请求")
public class PointRefundReturnDTO {

    @Schema(description = "退款订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退款订单号不能为空")
    private String refundOrderNo;

    @Schema(description = "原始订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始订单号不能为空")
    private String originalOrderNo;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "返还积分数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "返还积分数量不能为空")
    @Positive(message = "返还积分数量必须大于0")
    private Integer returnPoints;

    @Schema(description = "返还原因")
    private String reason;
}
