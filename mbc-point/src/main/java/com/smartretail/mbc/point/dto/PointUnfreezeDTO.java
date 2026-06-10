package com.smartretail.mbc.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "积分解冻请求")
public class PointUnfreezeDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "解冻积分数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "积分数量不能为空")
    @Positive(message = "积分数量必须大于0")
    private Integer points;

    @Schema(description = "来源ID")
    private String sourceId;

    @Schema(description = "备注")
    private String remark;
}
