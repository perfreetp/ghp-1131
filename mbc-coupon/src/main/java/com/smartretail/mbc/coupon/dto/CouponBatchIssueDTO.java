package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量发券请求")
public class CouponBatchIssueDTO {

    @NotEmpty(message = "会员ID列表不能为空")
    @Schema(description = "会员ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberIds;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "券模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long templateId;

    @Schema(description = "领取来源")
    private String receiveSource;

    @Schema(description = "来源ID")
    private Long sourceId;
}
