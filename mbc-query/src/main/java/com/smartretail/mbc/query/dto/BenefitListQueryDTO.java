package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "个人权益清单查询请求")
public class BenefitListQueryDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @Schema(description = "是否包含已过期", defaultValue = "false")
    private Boolean includeExpired = false;
}
