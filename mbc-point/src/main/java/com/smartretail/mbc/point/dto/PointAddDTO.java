package com.smartretail.mbc.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "积分发放请求")
public class PointAddDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "发放积分数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "积分数量不能为空")
    @Positive(message = "积分数量必须大于0")
    private Integer points;

    @Schema(description = "来源类型：1消费 2签到 3生日赠送 4注册赠送 5退款返还 6后台调整", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源类型不能为空")
    @Min(value = 1, message = "来源类型范围1-6")
    @Max(value = 6, message = "来源类型范围1-6")
    private Integer sourceType;

    @Schema(description = "来源ID")
    private String sourceId;

    @Schema(description = "过期天数，默认365天", defaultValue = "365")
    private Integer expireDays = 365;

    @Schema(description = "备注")
    private String remark;
}
