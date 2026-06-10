package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "活动预算总进度")
public class ActivityBudgetProgressVO {

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "总预算金额")
    private BigDecimal totalBudget;

    @Schema(description = "总已使用预算")
    private BigDecimal totalUsedBudget;

    @Schema(description = "总剩余预算")
    private BigDecimal totalRemainBudget;

    @Schema(description = "总使用率(%)")
    private BigDecimal totalUsageRate;

    @Schema(description = "各门店预算")
    private List<ActivityBudgetVO> storeBudgets;

    @Schema(description = "总发券上限")
    private Integer issueLimit;

    @Schema(description = "总已发券")
    private Integer totalIssued;

    @Schema(description = "总剩余发券")
    private Integer remainIssue;

    @Schema(description = "总核销上限")
    private Integer redeemLimit;

    @Schema(description = "总已核销")
    private Integer totalRedeemed;

    @Schema(description = "总剩余核销")
    private Integer remainRedeem;

    @Schema(description = "最近变动日志")
    private List<ActivityBudgetVO.BudgetLogItem> recentLogs;
}
