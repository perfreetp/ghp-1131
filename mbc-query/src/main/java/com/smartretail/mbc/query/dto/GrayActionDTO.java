package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "灰度操作请求")
public class GrayActionDTO {

    @NotNull(message = "灰度规则ID不能为空")
    @Schema(description = "灰度规则ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long grayRuleId;

    @NotBlank(message = "操作人不能为空")
    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operator;

    @Schema(description = "备注")
    private String remark;
}
