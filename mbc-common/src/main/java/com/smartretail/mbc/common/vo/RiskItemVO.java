package com.smartretail.mbc.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "风险项")
public class RiskItemVO {

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "当前值")
    private String currentValue;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "风险等级")
    private Integer riskLevel;
}
