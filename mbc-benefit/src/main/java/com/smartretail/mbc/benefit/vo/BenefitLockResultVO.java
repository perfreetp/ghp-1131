package com.smartretail.mbc.benefit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "锁定权益结果")
public class BenefitLockResultVO {

    @Schema(description = "锁定编号")
    private String useNo;

    @Schema(description = "权益类型 1优惠券 2积分抵扣 3等级折扣 4兑换权益")
    private Integer benefitType;

    @Schema(description = "权益抵扣金额（减少多少）")
    private BigDecimal benefitValue;

    @Schema(description = "已锁定的券实例ID列表")
    private List<Long> reducedCouponIds;

    @Schema(description = "已使用的积分数")
    private Integer usedPoints;

    @Schema(description = "等级折扣节省金额")
    private BigDecimal levelDiscountSaved;

    @Schema(description = "是否为幂等返回")
    private Boolean idempotent;

    @Schema(description = "本次处理唯一ID")
    private String requestId;

    @Schema(description = "处理状态 1处理中 2已完成 3失败")
    private Integer processStatus;
}
