package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "离线记录查询DTO")
public class OfflineRecordQueryDTO {

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "同步状态 0待同步 1同步中 2同步成功 3同步失败")
    private Integer syncStatus;

    @Schema(description = "页码 默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数 默认10")
    private Integer pageSize = 10;
}
