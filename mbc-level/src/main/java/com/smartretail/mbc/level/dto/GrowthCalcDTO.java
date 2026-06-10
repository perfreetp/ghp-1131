package com.smartretail.mbc.level.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "成长值计算请求")
public class GrowthCalcDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "订单金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "来源类型：1消费 2活动 3其他", defaultValue = "1")
    private Integer sourceType = 1;

    @Schema(description = "来源ID")
    private String sourceId;
}
