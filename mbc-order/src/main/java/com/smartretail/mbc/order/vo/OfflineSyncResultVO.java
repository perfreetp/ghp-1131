package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "离线补传结果VO")
public class OfflineSyncResultVO {

    @Schema(description = "离线锁号")
    private String offlineLockNo;

    @Schema(description = "同步状态 0待同步 1同步中 2同步成功 3同步失败")
    private Integer syncStatus;

    @Schema(description = "同步状态名称")
    private String syncStatusName;

    @Schema(description = "是否幂等返回")
    private Boolean isIdempotent;

    @Schema(description = "正式订单号")
    private String orderNo;

    @Schema(description = "权益使用编号")
    private String useNo;

    @Schema(description = "说明")
    private String message;
}
