package com.smartretail.mbc.level.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "等级调整请求")
public class LevelAdjustDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "调整成长值（可为负数）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调整成长值不能为空")
    private Integer adjustGrowth;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "调整原因")
    private String reason;
}
