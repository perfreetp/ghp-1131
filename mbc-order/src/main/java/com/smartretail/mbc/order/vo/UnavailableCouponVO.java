package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "不可用券及原因")
public class UnavailableCouponVO {

    @Schema(description = "券实例ID")
    private Long instanceId;

    @Schema(description = "券名称")
    private String couponName;

    @Schema(description = "券类型")
    private Integer couponType;

    @Schema(description = "不可用原因，如\"此券仅限大卖场使用，当前为便利店\"")
    private String reason;
}
