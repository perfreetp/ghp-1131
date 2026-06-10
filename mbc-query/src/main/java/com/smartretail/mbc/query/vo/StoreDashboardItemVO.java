package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "门店维度数据VO")
public class StoreDashboardItemVO {

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "门店等级")
    private Integer storeLevel;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "发券数")
    private Integer couponCount;

    @Schema(description = "核销数")
    private Integer redeemCount;

    @Schema(description = "核销金额")
    private BigDecimal redeemAmount;

    @Schema(description = "退款数")
    private Integer refundCount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "风险数")
    private Integer riskCount;

    @Schema(description = "已用预算")
    private BigDecimal budgetUsed;

    @Schema(description = "总预算")
    private BigDecimal budgetTotal;

    @Schema(description = "预算使用率")
    private BigDecimal budgetUsageRate;

    @Schema(description = "核销率")
    private BigDecimal redeemRate;

    @Schema(description = "排名")
    private Integer rank;

    @Schema(description = "环比: 1上升 -1下降 0持平")
    private Integer trend;

    @Schema(description = "变化率")
    private BigDecimal changeRate;
}
