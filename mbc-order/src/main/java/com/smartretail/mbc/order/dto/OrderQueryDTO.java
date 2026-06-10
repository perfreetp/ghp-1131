package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "订单查询请求")
public class OrderQueryDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "页码 默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小 默认10")
    private Integer pageSize = 10;
}
