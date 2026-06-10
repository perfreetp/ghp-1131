package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "活动维度数据VO")
public class ActivityDashboardItemVO {

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动编码")
    private String activityCode;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "活动类型")
    private Integer activityType;

    @Schema(description = "发券数")
    private Integer couponCount;

    @Schema(description = "核销数")
    private Integer redeemCount;

    @Schema(description = "核销金额")
    private BigDecimal redeemAmount;

    @Schema(description = "带动订单数")
    private Integer drivenOrderCount;

    @Schema(description = "带动订单金额")
    private BigDecimal drivenOrderAmount;

    @Schema(description = "已用预算")
    private BigDecimal budgetUsed;

    @Schema(description = "总预算")
    private BigDecimal budgetTotal;

    @Schema(description = "预算使用率")
    private BigDecimal budgetUsageRate;

    @Schema(description = "活动状态")
    private Integer status;

    @Schema(description = "剩余天数")
    private Integer daysLeft;
}
