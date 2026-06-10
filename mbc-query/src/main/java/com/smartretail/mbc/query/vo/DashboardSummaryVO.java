package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "经营驾驶舱顶部汇总卡片VO")
public class DashboardSummaryVO {

    @Schema(description = "累计发券数")
    private Integer totalCouponCount;

    @Schema(description = "累计核销数")
    private Integer totalRedeemCount;

    @Schema(description = "累计核销权益金额")
    private BigDecimal totalRedeemAmount;

    @Schema(description = "累计退款笔数")
    private Integer totalRefundCount;

    @Schema(description = "累计退款金额")
    private BigDecimal totalRefundAmount;

    @Schema(description = "累计风险拦截数")
    private Integer totalRiskCount;

    @Schema(description = "已用预算")
    private BigDecimal totalBudgetUsed;

    @Schema(description = "剩余预算")
    private BigDecimal totalBudgetRemain;

    @Schema(description = "预算使用率")
    private BigDecimal budgetUsageRate;

    @Schema(description = "核销率")
    private BigDecimal redeemRate;

    @Schema(description = "退款率")
    private BigDecimal refundRate;
}
