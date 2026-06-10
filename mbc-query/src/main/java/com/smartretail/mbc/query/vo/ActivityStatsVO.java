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
@Schema(description = "活动效果统计VO")
public class ActivityStatsVO extends Activity {

    @Schema(description = "预算使用率")
    private BudgetUsage budgetUsage;

    @Schema(description = "漏斗转化")
    private Funnel funnel;

    @Schema(description = "投资回报率 ROI = 带动订单金额 / (使用积分/100 + 券估算价值)")
    private BigDecimal roi;

    @Schema(description = "参与者等级分布 Map<levelCode, count>")
    private Map<String, Integer> participantLevels;

    @Schema(description = "每日趋势")
    private List<DailyDataItem> dailyTrend;

    @Data
    @Schema(description = "预算使用率")
    public static class BudgetUsage {

        @Schema(description = "积分使用率(%)")
        private BigDecimal pointsUsedPercent;

        @Schema(description = "券使用率(%)")
        private BigDecimal couponsUsedPercent;
    }

    @Data
    @Schema(description = "漏斗转化")
    public static class Funnel {

        @Schema(description = "曝光率(%)")
        private BigDecimal exposureRate;

        @Schema(description = "参与率(%) 参与/曝光")
        private BigDecimal participationRate;

        @Schema(description = "转化率(%) 转化/参与")
        private BigDecimal conversionRate;
    }

    @Data
    @Schema(description = "每日趋势数据项")
    public static class DailyDataItem {

        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "新增参与人数")
        private Integer newParticipants;

        @Schema(description = "新增转化人数")
        private Integer newConversions;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;
    }
}
