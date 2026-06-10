package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "人群效果对比VO")
public class CrowdEffectCompareVO {

    @Schema(description = "人群ID")
    private Long crowdId;

    @Schema(description = "人群名称")
    private String crowdName;

    @Schema(description = "会员数量")
    private Integer memberCount;

    @Schema(description = "参与人数")
    private Integer participateCount;

    @Schema(description = "参与率(%)")
    private BigDecimal participateRate;

    @Schema(description = "核销人数")
    private Integer verifyCount;

    @Schema(description = "核销率(%)")
    private BigDecimal verifyRate;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "客单价")
    private BigDecimal avgOrderAmount;

    @Schema(description = "人均投入")
    private BigDecimal costValue;

    @Schema(description = "投资回报率ROI")
    private BigDecimal roi;
}
