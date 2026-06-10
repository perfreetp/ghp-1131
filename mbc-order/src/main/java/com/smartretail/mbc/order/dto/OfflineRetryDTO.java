package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "离线批量重试DTO")
public class OfflineRetryDTO {

    @Schema(description = "门店编码")
    private String storeCode;
}
