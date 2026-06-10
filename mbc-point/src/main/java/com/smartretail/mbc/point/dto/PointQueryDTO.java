package com.smartretail.mbc.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "积分流水查询请求")
public class PointQueryDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "积分类型：1增加 2扣减 3冻结 4解冻")
    private Integer pointType;

    @Schema(description = "来源类型：1消费 2签到 3生日赠送 4注册赠送 5退款返还 6后台调整")
    private Integer sourceType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量，默认20", defaultValue = "20")
    private Integer pageSize = 20;
}
