package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "小程序优惠券信息")
public class MiniCouponVO {

    @Schema(description = "券实例ID")
    private Long instanceId;

    @Schema(description = "券模板ID")
    private Long templateId;

    @Schema(description = "券名称")
    private String couponName;

    @Schema(description = "券类型名称")
    private String couponTypeName;

    @Schema(description = "券类型图标")
    private String couponTypeIcon;

    @Schema(description = "券状态")
    private Integer status;

    @Schema(description = "券状态名称")
    private String statusName;

    @Schema(description = "满减门槛金额")
    private BigDecimal fullAmount;

    @Schema(description = "减免金额")
    private BigDecimal reduceAmount;

    @Schema(description = "兑换商品")
    private String exchangeItem;

    @Schema(description = "生效开始时间")
    private LocalDateTime validStart;

    @Schema(description = "生效结束时间")
    private LocalDateTime validEnd;

    @Schema(description = "距过期天数")
    private Integer daysLeft;

    @Schema(description = "7天内过期标记")
    private Boolean expireTag;
}
