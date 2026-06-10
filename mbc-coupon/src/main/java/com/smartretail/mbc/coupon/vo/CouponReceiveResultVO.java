package com.smartretail.mbc.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "领券结果VO")
public class CouponReceiveResultVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "券实例ID")
    private Long instanceId;

    @Schema(description = "券实例编号")
    private String instanceNo;

    @Schema(description = "失败原因")
    private String failReason;
}
