package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "对账明细VO")
public class ReconcileDetailVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "权益使用编号")
    private String useNo;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "权益类型")
    private Integer benefitType;

    @Schema(description = "权益金额")
    private BigDecimal benefitValue;

    @Schema(description = "使用积分")
    private Integer usedPoints;

    @Schema(description = "使用状态")
    private Integer useStatus;

    @Schema(description = "使用状态名称")
    private String useStatusName;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "锁定时间")
    private LocalDateTime lockTime;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "返还时间")
    private LocalDateTime returnTime;

    @Schema(description = "收银端实际支付金额")
    private BigDecimal posPayAmount;

    @Schema(description = "收银端支付时间")
    private LocalDateTime posPayTime;

    @Schema(description = "对账状态")
    private Integer reconcileStatus;

    @Schema(description = "对账状态名称")
    private String reconcileStatusName;

    @Schema(description = "差异描述")
    private String reconcileDiff;

    @Schema(description = "对账时间")
    private LocalDateTime reconcileTime;
}
