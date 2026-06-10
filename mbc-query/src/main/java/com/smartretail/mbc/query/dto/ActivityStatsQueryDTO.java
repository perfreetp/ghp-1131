package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "活动效果统计查询请求")
public class ActivityStatsQueryDTO {

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动类型")
    private Integer activityType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "活动状态")
    private Integer status;

    @Schema(description = "页码 默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小 默认10")
    private Integer pageSize = 10;
}
