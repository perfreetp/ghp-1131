package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "修改人群组请求")
public class CrowdGroupUpdateDTO {

    @NotNull(message = "人群ID不能为空")
    @Schema(description = "人群ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long crowdId;

    @Schema(description = "人群名称")
    private String crowdName;

    @Schema(description = "圈选规则列表")
    private List<CrowdGroupCreateDTO.CrowdRuleItem> rules;

    @Schema(description = "描述")
    private String description;
}
