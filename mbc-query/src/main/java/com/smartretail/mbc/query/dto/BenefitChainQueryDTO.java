package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "权益链路查询请求")
public class BenefitChainQueryDTO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "退款单号")
    private String refundNo;

    public boolean hasAtLeastOne() {
        return (orderNo != null && !orderNo.isBlank())
                || (refundNo != null && !refundNo.isBlank());
    }
}
