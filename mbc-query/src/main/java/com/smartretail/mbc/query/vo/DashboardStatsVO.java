package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "运营大盘VO")
public class DashboardStatsVO {

    @Schema(description = "会员统计")
    private MemberStats memberStats;

    @Schema(description = "订单统计")
    private OrderStats orderStats;

    @Schema(description = "权益统计")
    private BenefitStats benefitStats;

    @Schema(description = "Top5活动列表")
    private List<ActivityStatsVO> topActivities;

    @Schema(description = "等级分布")
    private List<LevelDistributionItem> levelDistribution;

    @Data
    @Schema(description = "会员统计")
    public static class MemberStats {

        @Schema(description = "会员总数")
        private Long totalMembers;

        @Schema(description = "本月新增")
        private Long newThisMonth;

        @Schema(description = "本月活跃")
        private Long activeThisMonth;

        @Schema(description = "按等级分布 Map<levelCode, count>")
        private Map<String, Long> byLevel;
    }

    @Data
    @Schema(description = "订单统计")
    public static class OrderStats {

        @Schema(description = "订单总数")
        private Long totalOrders;

        @Schema(description = "总支付金额")
        private BigDecimal totalPayAmount;

        @Schema(description = "客单价")
        private BigDecimal avgOrderAmount;

        @Schema(description = "会员订单占比(%)")
        private BigDecimal memberOrderRatio;
    }

    @Data
    @Schema(description = "权益统计")
    public static class BenefitStats {

        @Schema(description = "发放券总数")
        private Long totalCouponsIssued;

        @Schema(description = "使用券总数")
        private Long totalCouponsUsed;

        @Schema(description = "券使用率(%)")
        private BigDecimal couponUsageRate;

        @Schema(description = "发放积分总数")
        private Long totalPointsIssued;

        @Schema(description = "兑换使用积分总数")
        private Long totalPointsRedeemed;
    }

    @Data
    @Schema(description = "等级分布项")
    public static class LevelDistributionItem {

        @Schema(description = "等级编码")
        private Integer levelCode;

        @Schema(description = "等级名称")
        private String levelName;

        @Schema(description = "人数")
        private Long count;

        @Schema(description = "占比(%)")
        private BigDecimal ratio;
    }
}
