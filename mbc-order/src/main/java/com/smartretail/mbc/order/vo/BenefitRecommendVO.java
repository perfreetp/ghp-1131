package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "推荐方案")
public class BenefitRecommendVO {

    @Schema(description = "方案编号，如1/2/3")
    private Integer planId;

    @Schema(description = "方案名，如\"使用满200减30券+500积分\"")
    private String planName;

    @Schema(description = "使用的券ID列表")
    private List<Long> couponIds;

    @Schema(description = "券名列表")
    private List<String> couponNames;

    @Schema(description = "券节省金额")
    private BigDecimal couponSavedAmount;

    @Schema(description = "使用积分数")
    private Integer usedPoints;

    @Schema(description = "积分节省金额")
    private BigDecimal pointSavedAmount;

    @Schema(description = "总共节省金额")
    private BigDecimal totalSavedAmount;

    @Schema(description = "推荐原因，如\"满200减30券最划算，加上500积分可再抵5元\"")
    private String reason;

    @Schema(description = "推荐优先级，1=最优")
    private Integer rank;
}
