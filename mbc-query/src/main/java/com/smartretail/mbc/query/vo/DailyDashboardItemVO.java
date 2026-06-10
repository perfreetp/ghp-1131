package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "每日趋势数据VO")
public class DailyDashboardItemVO {

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "发券数")
    private Integer couponCount;

    @Schema(description = "核销数")
    private Integer redeemCount;

    @Schema(description = "核销金额")
    private BigDecimal redeemAmount;

    @Schema(description = "退款数")
    private Integer refundCount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "风险数")
    private Integer riskCount;

    @Schema(description = "预算使用")
    private BigDecimal budgetUsed;
}
