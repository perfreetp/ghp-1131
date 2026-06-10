package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "灰度效果查询请求")
public class GrayEffectQueryDTO {

    @NotNull(message = "灰度规则ID不能为空")
    @Schema(description = "灰度规则ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long grayRuleId;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "分组维度: date/level/store")
    private String groupBy = "date";
}
