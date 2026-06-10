package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "经营驾驶舱总览VO")
public class OperationDashboardVO {

    @Schema(description = "顶部汇总卡片")
    private DashboardSummaryVO summary;

    @Schema(description = "门店排名")
    private List<StoreDashboardItemVO> storeRank;

    @Schema(description = "活动排名")
    private List<ActivityDashboardItemVO> activityRank;

    @Schema(description = "等级分布")
    private List<LevelDashboardItemVO> levelDistribution;

    @Schema(description = "异常波动门店")
    private List<AbnormalStoreVO> abnormalStores;

    @Schema(description = "近N天趋势")
    private List<DailyDashboardItemVO> recentTrend;
}
