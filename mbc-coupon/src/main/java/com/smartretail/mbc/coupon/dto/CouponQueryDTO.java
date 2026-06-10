package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户券查询请求")
public class CouponQueryDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "券状态")
    private Integer couponStatus;

    @Schema(description = "有效期开始范围-起始")
    private LocalDateTime validEndStart;

    @Schema(description = "有效期开始范围-结束")
    private LocalDateTime validEndEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
