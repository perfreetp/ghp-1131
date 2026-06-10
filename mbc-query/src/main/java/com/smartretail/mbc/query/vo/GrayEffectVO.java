package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "灰度效果对比VO")
public class GrayEffectVO {

    @Schema(description = "灰度组数据")
    private GrayMetricItemVO grayGroup;

    @Schema(description = "对照组数据")
    private GrayMetricItemVO controlGroup;

    @Schema(description = "对比数据")
    private GrayComparisonVO comparison;

    @Schema(description = "每日数据(用于画图)")
    private List<GrayDailyItemVO> dailyData;
}
