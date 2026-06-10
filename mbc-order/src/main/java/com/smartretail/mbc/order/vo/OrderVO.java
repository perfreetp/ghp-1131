package com.smartretail.mbc.order.vo;

import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单详情VO")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "订单状态名称")
    private String orderStatusName;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠总金额")
    private BigDecimal discountAmount;

    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponAmount;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointAmount;

    @Schema(description = "等级折扣金额")
    private BigDecimal levelDiscount;

    @Schema(description = "实际支付金额")
    private BigDecimal payAmount;

    @Schema(description = "获得积分")
    private Integer earnedPoints;

    @Schema(description = "获得成长值")
    private Integer earnedGrowth;

    @Schema(description = "使用积分")
    private Integer usedPoints;

    @Schema(description = "使用的券ID")
    private String usedCouponIds;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "收银员")
    private String cashier;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "会员简单信息")
    private MemberSimpleVO memberInfo;

    @Schema(description = "是否为幂等返回")
    private Boolean idempotent;

    @Schema(description = "本次处理唯一ID")
    private String requestId;

    @Schema(description = "处理状态 1处理中 2已完成 3失败")
    private Integer processStatus;

    @Schema(description = "风控检查结果")
    private RiskCheckResultVO riskCheck;
}
