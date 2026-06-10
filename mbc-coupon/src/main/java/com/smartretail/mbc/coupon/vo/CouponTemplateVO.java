package com.smartretail.mbc.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "优惠券模板VO")
public class CouponTemplateVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "券编码")
    private String couponCode;

    @Schema(description = "券名称")
    private String couponName;

    @Schema(description = "券类型")
    private Integer couponType;

    @Schema(description = "券类型名称")
    private String couponTypeName;

    @Schema(description = "发放总量")
    private Integer totalAmount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "已使用数量")
    private Integer usedCount;

    @Schema(description = "剩余数量")
    private Integer remainCount;

    @Schema(description = "满减门槛金额")
    private BigDecimal fullAmount;

    @Schema(description = "减免金额")
    private BigDecimal reduceAmount;

    @Schema(description = "兑换商品")
    private String exchangeItem;

    @Schema(description = "有效期类型")
    private Integer validType;

    @Schema(description = "有效期开始")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束")
    private LocalDateTime validEnd;

    @Schema(description = "有效天数")
    private Integer validDays;

    @Schema(description = "最低会员等级")
    private Integer minLevel;

    @Schema(description = "每日限领数量")
    private Integer dailyLimit;

    @Schema(description = "每人限领总数")
    private Integer totalLimit;

    @Schema(description = "适用场景")
    private String applyScenes;

    @Schema(description = "排除商品")
    private String excludeItems;

    @Schema(description = "是否可叠加")
    private Integer stackable;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "关联活动ID")
    private Long activityId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
