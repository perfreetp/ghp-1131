package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建人群组请求")
public class CrowdGroupCreateDTO {

    @NotBlank(message = "人群编码不能为空")
    @Schema(description = "人群编码(唯一)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String crowdCode;

    @NotBlank(message = "人群名称不能为空")
    @Schema(description = "人群名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String crowdName;

    @NotNull(message = "人群类型不能为空")
    @Schema(description = "人群类型：1静态 2动态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer crowdType;

    @Schema(description = "圈选规则列表")
    private List<CrowdRuleItem> rules;

    @Schema(description = "描述")
    private String description;

    @Data
    @Schema(description = "圈选规则项")
    public static class CrowdRuleItem {

        @Schema(description = "规则类型")
        private Integer ruleType;

        @Schema(description = "操作符：>,>=,<,<=,=,in,between")
        private String operator;

        @Schema(description = "最小值")
        private Object valueMin;

        @Schema(description = "最大值")
        private Object valueMax;

        @Schema(description = "值列表")
        private List<Object> values;
    }
}
