package com.smartretail.mbc.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "积分冻结请求")
public class PointFreezeDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "冻结积分数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "积分数量不能为空")
    @Positive(message = "积分数量必须大于0")
    private Integer points;

    @Schema(description = "冻结订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "冻结订单号不能为空")
    private String sourceId;

    @Schema(description = "备注")
    private String remark;
}
