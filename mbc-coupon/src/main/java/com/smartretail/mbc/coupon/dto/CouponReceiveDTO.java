package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "领券请求")
public class CouponReceiveDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "券模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long templateId;

    @Schema(description = "领取来源")
    private String receiveSource;

    @Schema(description = "来源ID")
    private Long sourceId;
}
