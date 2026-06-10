package com.smartretail.mbc.level.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "生日权益发放请求")
public class BirthdayBenefitDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "覆盖默认券ID列表（可空）")
    private List<Long> templateIds;
}
