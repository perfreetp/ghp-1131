package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "幂等记录人工处理请求")
public class IdempotentHandleDTO {

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记录ID不能为空")
    private Long id;

    @Schema(description = "操作类型: 1人工重放 2标记失败", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作类型不能为空")
    private Integer action;

    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作人不能为空")
    private String operator;

    @Schema(description = "备注")
    private String remark;
}
