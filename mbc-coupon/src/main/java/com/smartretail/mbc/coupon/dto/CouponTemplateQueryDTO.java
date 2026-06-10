package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "券模板查询请求")
public class CouponTemplateQueryDTO {

    @Schema(description = "券类型")
    private Integer couponType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "关键词(券名称/编码)")
    private String keyword;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
