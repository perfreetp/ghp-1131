package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "消费记录查询请求")
public class ConsumeRecordQueryDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "最小金额")
    private BigDecimal minAmount;

    @Schema(description = "最大金额")
    private BigDecimal maxAmount;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "页码 默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小 默认10")
    private Integer pageSize = 10;
}
