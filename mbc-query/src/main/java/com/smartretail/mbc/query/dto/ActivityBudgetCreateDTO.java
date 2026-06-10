package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建活动预算请求")
public class ActivityBudgetCreateDTO {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @Schema(description = "门店编码(空=总部总预算)")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @NotNull(message = "总预算金额不能为空")
    @Schema(description = "总预算金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalBudget;

    @Schema(description = "发券上限")
    private Integer issueLimit;

    @Schema(description = "核销上限")
    private Integer redeemLimit;
}
