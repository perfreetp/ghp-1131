package com.smartretail.mbc.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "风控检查结果")
public class RiskCheckResultVO {

    @Schema(description = "是否放行")
    private Boolean pass;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "风险等级名称")
    private String riskLevelName;

    @Schema(description = "风控场景")
    private Integer scene;

    @Schema(description = "风控场景名称")
    private String sceneName;

    @Schema(description = "命中的风险项")
    private List<RiskItemVO> riskItems;

    @Schema(description = "处置建议: 放行/人工确认/拦截")
    private String advice;
}
