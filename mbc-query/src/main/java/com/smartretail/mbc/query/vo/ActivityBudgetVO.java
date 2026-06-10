package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "活动预算视图")
public class ActivityBudgetVO {

    @Schema(description = "预算ID")
    private Long id;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "预算类型: 1总预算 2门店预算")
    private Integer budgetType;

    @Schema(description = "预算类型名称")
    private String budgetTypeName;

    @Schema(description = "总预算金额")
    private BigDecimal totalBudget;

    @Schema(description = "已使用预算")
    private BigDecimal usedBudget;

    @Schema(description = "剩余预算")
    private BigDecimal remainBudget;

    @Schema(description = "使用率(%)")
    private BigDecimal usageRate;

    @Schema(description = "发券上限")
    private Integer issueLimit;

    @Schema(description = "已发券数")
    private Integer issuedCount;

    @Schema(description = "剩余发券")
    private Integer remainIssue;

    @Schema(description = "发券率(%)")
    private BigDecimal issueRate;

    @Schema(description = "核销上限")
    private Integer redeemLimit;

    @Schema(description = "已核销数")
    private Integer redeemedCount;

    @Schema(description = "剩余核销")
    private Integer remainRedeem;

    @Schema(description = "核销率(%)")
    private BigDecimal redeemRate;

    @Schema(description = "状态: 0正常 1超限")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "最近变动日志")
    private List<BudgetLogItem> recentLogs;

    @Data
    @Schema(description = "预算变动日志项")
    public static class BudgetLogItem {

        @Schema(description = "变动类型: 1领取占用 2锁定占用 3核销确认 4退款释放 5超时释放")
        private Integer changeType;

        @Schema(description = "变动类型名称")
        private String changeTypeName;

        @Schema(description = "变动金额")
        private BigDecimal changeAmount;

        @Schema(description = "变动数量")
        private Integer changeQuantity;

        @Schema(description = "关联订单号")
        private String orderNo;

        @Schema(description = "变动时间")
        private LocalDateTime createTime;
    }
}
