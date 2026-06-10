package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "对账明细查询请求")
public class ReconcileDetailQueryDTO {

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "券模板ID")
    private Long templateId;

    @Schema(description = "对账状态")
    private Integer reconcileStatus;

    @Schema(description = "订单号")
    private String orderNo;

    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate endDate;

    @Schema(description = "页码 默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小 默认20")
    private Integer pageSize = 20;
}
