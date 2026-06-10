package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "灰度对比差异VO")
public class GrayComparisonVO {

    @Schema(description = "核销金额差")
    private BigDecimal redeemAmountDiff;

    @Schema(description = "核销金额变化率%")
    private BigDecimal redeemAmountRatio;

    @Schema(description = "订单金额差")
    private BigDecimal orderAmountDiff;

    @Schema(description = "订单金额变化率%")
    private BigDecimal orderAmountRatio;

    @Schema(description = "转化率差")
    private BigDecimal conversionDiff;

    @Schema(description = "退款率差")
    private BigDecimal refundDiff;

    @Schema(description = "是否灰度组更优")
    private Boolean isBetter;
}
