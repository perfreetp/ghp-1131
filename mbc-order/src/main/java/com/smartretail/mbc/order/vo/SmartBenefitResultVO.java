package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "智能权益推荐结果")
public class SmartBenefitResultVO {

    @Schema(description = "会员等级编码")
    private Integer memberLevel;

    @Schema(description = "会员等级名称")
    private String memberLevelName;

    @Schema(description = "当前积分")
    private Integer currentPoints;

    @Schema(description = "可用券数")
    private Integer availableCoupons;

    @Schema(description = "总券数")
    private Integer totalCoupons;

    @Schema(description = "推荐方案列表")
    private List<BenefitRecommendVO> recommendations;

    @Schema(description = "最优推荐方案")
    private BenefitRecommendVO bestRecommend;

    @Schema(description = "不可用券及原因")
    private List<UnavailableCouponVO> unavailableCoupons;
}
