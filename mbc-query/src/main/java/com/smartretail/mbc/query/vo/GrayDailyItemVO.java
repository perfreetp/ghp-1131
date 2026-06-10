package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "每日对比数据VO")
public class GrayDailyItemVO {

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "灰度组核销金额")
    private BigDecimal grayRedeemAmount;

    @Schema(description = "对照组核销金额")
    private BigDecimal controlRedeemAmount;

    @Schema(description = "灰度组转化率")
    private BigDecimal grayConversionRate;

    @Schema(description = "对照组转化率")
    private BigDecimal controlConversionRate;
}
