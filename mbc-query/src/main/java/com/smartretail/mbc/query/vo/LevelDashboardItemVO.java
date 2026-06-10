package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "会员等级维度数据VO")
public class LevelDashboardItemVO {

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "会员数")
    private Integer memberCount;

    @Schema(description = "发券数")
    private Integer couponCount;

    @Schema(description = "核销数")
    private Integer redeemCount;

    @Schema(description = "核销金额")
    private BigDecimal redeemAmount;

    @Schema(description = "人均领券")
    private BigDecimal avgCouponPerMember;
}
