package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "查询群成员请求")
public class CrowdMemberQueryDTO {

    @NotNull(message = "人群ID不能为空")
    @Schema(description = "人群ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long crowdId;

    @Schema(description = "关键词搜索")
    private String keyword;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 10;
}
