package com.smartretail.mbc.benefit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "核销记录查询请求")
public class BenefitQueryDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "使用状态 1锁定 2核销成功 3已返还")
    private Integer useStatus;

    @Schema(description = "权益类型 1优惠券 2积分抵扣 3等级折扣 4兑换权益")
    private Integer benefitType;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认20")
    private Integer pageSize = 20;
}
