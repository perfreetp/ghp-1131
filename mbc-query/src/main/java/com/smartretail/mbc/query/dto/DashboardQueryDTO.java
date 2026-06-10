package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "经营驾驶舱查询请求DTO")
public class DashboardQueryDTO {

    @Schema(description = "维度类型: store/activity/level/date, 默认store")
    private String dimType = "store";

    @Schema(description = "省份筛选")
    private String province;

    @Schema(description = "城市筛选")
    private String city;

    @Schema(description = "门店筛选")
    private String storeCode;

    @Schema(description = "活动筛选")
    private Long activityId;

    @Schema(description = "等级筛选")
    private Integer levelCode;

    @Schema(description = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "排名前N, 默认20")
    private Integer rankTop = 20;

    @Schema(description = "趋势天数, 默认7天")
    private Integer trendDays = 7;
}
