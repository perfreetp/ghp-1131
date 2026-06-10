package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建灰度规则请求")
public class GrayRuleCreateDTO {

    @NotBlank(message = "灰度规则编码不能为空")
    @Schema(description = "灰度规则编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String grayCode;

    @NotBlank(message = "灰度规则名称不能为空")
    @Schema(description = "灰度规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String grayName;

    @NotNull(message = "关联活动ID不能为空")
    @Schema(description = "关联活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @NotNull(message = "灰度类型不能为空")
    @Schema(description = "灰度类型: 1城市灰度 2门店灰度 3人群灰度 4设备灰度", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer grayType;

    @Schema(description = "灰度配置JSON")
    private String grayConfig;

    @Schema(description = "灰度规则内容JSON(新规则配置)")
    private String ruleContent;

    @Schema(description = "灰度流量比例 0-100")
    private Integer grayRatio = 10;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "操作人")
    private String operator;
}
