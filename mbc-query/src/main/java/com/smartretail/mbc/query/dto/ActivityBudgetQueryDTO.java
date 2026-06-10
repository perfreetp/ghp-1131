package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "查询活动预算请求")
public class ActivityBudgetQueryDTO {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @Schema(description = "门店编码(可选)")
    private String storeCode;

    @Schema(description = "分组方式: store=按门店, all=汇总")
    private String groupBy;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;
}
