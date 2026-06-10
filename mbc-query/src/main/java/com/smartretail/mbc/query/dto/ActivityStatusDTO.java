package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "活动状态变更请求")
public class ActivityStatusDTO {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态：0草稿 1进行中 2已结束 3已取消", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetStatus;

    @Schema(description = "变更原因")
    private String reason;
}
