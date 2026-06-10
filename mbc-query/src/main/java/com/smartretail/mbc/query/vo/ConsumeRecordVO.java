package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "消费记录VO")
public class ConsumeRecordVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "订单类型名称")
    private String orderTypeName;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "获得积分")
    private Integer earnedPoints;

    @Schema(description = "获得成长值")
    private Integer earnedGrowth;

    @Schema(description = "优惠券金额")
    private BigDecimal couponAmount;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointAmount;

    @Schema(description = "等级折扣金额")
    private BigDecimal levelDiscount;

    @Schema(description = "使用积分")
    private Integer usedPoints;

    @Schema(description = "使用券数量")
    private Integer usedCoupons;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "渠道名称")
    private String channelName;
}
