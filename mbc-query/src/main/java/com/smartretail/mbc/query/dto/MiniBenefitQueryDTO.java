package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "小程序权益查询请求")
public class MiniBenefitQueryDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @Schema(description = "券状态筛选：1可用/4锁定/2已使用/3已过期/5已失效")
    private List<Integer> couponStatusFilter;

    @Schema(description = "是否包含已过期", defaultValue = "false")
    private Boolean includeExpired = false;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer pageSize = 20;
}
