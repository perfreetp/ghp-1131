package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "权益履约状态查询请求")
public class BenefitFulfillmentQueryDTO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "退款单号")
    private String refundNo;
}
