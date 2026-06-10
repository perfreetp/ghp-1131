package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "门店查询条件")
public class StoreQueryDTO {

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "业态类型编码")
    private Integer storeType;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10")
    private Integer pageSize = 10;
}
