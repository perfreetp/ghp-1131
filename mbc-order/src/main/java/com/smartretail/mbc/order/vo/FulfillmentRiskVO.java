package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "风险记录VO")
public class FulfillmentRiskVO {

    @Schema(description = "记录编号")
    private String recordNo;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "风险等级名称")
    private String riskLevelName;

    @Schema(description = "风险场景")
    private Integer scene;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "处理结果")
    private Integer handleResult;

    @Schema(description = "处理结果名称")
    private String handleResultName;

    @Schema(description = "处理人员")
    private String handleStaff;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理备注")
    private String handleRemark;
}
