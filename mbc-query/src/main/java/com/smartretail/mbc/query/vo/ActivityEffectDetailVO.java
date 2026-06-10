package com.smartretail.mbc.query.vo;

import com.smartretail.mbc.query.entity.Activity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "活动效果详情VO")
public class ActivityEffectDetailVO extends Activity {

    @Schema(description = "效果摘要")
    private EffectSummary effectSummary;

    @Schema(description = "每张关联券的效果")
    private List<CouponEffectVO> couponEffect;

    @Schema(description = "按等级统计 Map<levelCode, LevelEffect>")
    private Map<String, LevelEffect> memberLevelEffect;

    @Schema(description = "每日效果趋势")
    private List<DailyEffectItem> dailyTrend;

    @Schema(description = "退款影响分析")
    private RefundImpact refundImpact;

    @Schema(description = "关联人群信息")
    private CrowdInfo crowdInfo;

    @Schema(description = "人群效果对比列表")
    private List<CrowdEffectCompareVO> crowdEffectList;

    @Schema(description = "预算进度")
    private ActivityBudgetProgressVO budgetProgress;

    @Schema(description = "灰度效果数据")
    private GrayEffectVO grayEffect;

    @Data
    @Schema(description = "人群信息")
    public static class CrowdInfo {

        @Schema(description = "人群ID")
        private Long crowdId;

        @Schema(description = "人群名称")
        private String crowdName;

        @Schema(description = "人群总人数")
        private Integer totalCount;
    }

    @Data
    @Schema(description = "效果摘要")
    public static class EffectSummary {

        @Schema(description = "领取人数")
        private Integer receiveCount;

        @Schema(description = "领取率(%)")
        private BigDecimal receiveCountRate;

        @Schema(description = "核销人数")
        private Integer verifyCount;

        @Schema(description = "核销率(%)")
        private BigDecimal verifyCountRate;

        @Schema(description = "活动带来新会员数")
        private Long newMemberCount;

        @Schema(description = "总订单数")
        private Long totalOrderCount;

        @Schema(description = "总订单金额")
        private BigDecimal totalOrderAmount;

        @Schema(description = "退款订单数")
        private Long refundOrderCount;

        @Schema(description = "退款金额")
        private BigDecimal refundOrderAmount;

        @Schema(description = "退款占比(%)")
        private BigDecimal refundRatio;

        @Schema(description = "净订单金额=总-退款")
        private BigDecimal netOrderAmount;

        @Schema(description = "消耗积分")
        private Integer costPoints;

        @Schema(description = "消耗券数量")
        private Integer costCouponCount;

        @Schema(description = "总投入成本=积分/100 + 券估算价值")
        private BigDecimal totalCost;

        @Schema(description = "投资回报率 ROI=净订单金额/总投入成本")
        private BigDecimal roi;
    }

    @Data
    @Schema(description = "单张券效果")
    public static class CouponEffectVO {

        @Schema(description = "券模板ID")
        private Long templateId;

        @Schema(description = "券名称")
        private String couponName;

        @Schema(description = "已发放数量")
        private Integer issuedCount;

        @Schema(description = "已使用数量")
        private Integer usedCount;

        @Schema(description = "未使用数量")
        private Integer unusedCount;

        @Schema(description = "已过期数量")
        private Integer expiredCount;

        @Schema(description = "带动订单金额")
        private BigDecimal usedOrderAmount;

        @Schema(description = "使用率(%)")
        private BigDecimal usageRate;
    }

    @Data
    @Schema(description = "等级效果统计")
    public static class LevelEffect {

        @Schema(description = "等级编码")
        private String levelCode;

        @Schema(description = "等级名称")
        private String levelName;

        @Schema(description = "参与人数")
        private Integer participateCount;

        @Schema(description = "核销人数")
        private Integer verifyCount;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;
    }

    @Data
    @Schema(description = "每日效果数据项")
    public static class DailyEffectItem {

        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "新增领取人数")
        private Integer newReceive;

        @Schema(description = "新增核销人数")
        private Integer newVerify;

        @Schema(description = "订单数")
        private Integer orderCount;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;

        @Schema(description = "退款数")
        private Integer refundCount;

        @Schema(description = "退款金额")
        private BigDecimal refundAmount;
    }

    @Data
    @Schema(description = "退款影响分析")
    public static class RefundImpact {

        @Schema(description = "总退款单数")
        private Long totalRefundCount;

        @Schema(description = "总退款金额")
        private BigDecimal totalRefundAmount;

        @Schema(description = "退款原因分布 Map<reason, count>")
        private Map<String, Integer> refundByReason;
    }
}
