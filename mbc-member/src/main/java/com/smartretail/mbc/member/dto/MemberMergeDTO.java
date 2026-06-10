package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "会员合并请求")
public class MemberMergeDTO {

    @Schema(description = "被合并会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "被合并会员ID不能为空")
    private Long sourceMemberId;

    @Schema(description = "目标会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标会员ID不能为空")
    private Long targetMemberId;

    @Schema(description = "合并原因")
    private String reason;

    @Schema(description = "操作人")
    private String operator;
}
