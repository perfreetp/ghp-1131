package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "计算人群请求")
public class CrowdCalcDTO {

    @NotNull(message = "人群ID不能为空")
    @Schema(description = "人群ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long crowdId;
}
