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

    @Schema(description = "适用门店编码列表(逗号分隔，空=全部适用)")
    private String applyStoreCodes;

    @Schema(description = "排除门店编码列表(逗号分隔)")
    private String excludeStoreCodes;

    @Schema(description = "适用业态列表(逗号分隔，如\"1,2,3\"，空=全业态)")
    private String applyBusinessTypes;

    @Schema(description = "适用收银设备类型(逗号分隔，空=全部支持)")
    private String applyPosTypes;

    @Schema(description = "门店限制模式：0=白名单模式 1=黑名单模式")
    private Integer storeLimitFlag;

    @Schema(description = "适用门店名称列表")
    private java.util.List<String> applyStoreNames;

    @Schema(description = "适用业态名称列表")
    private java.util.List<String> applyBusinessTypeNames;
}
