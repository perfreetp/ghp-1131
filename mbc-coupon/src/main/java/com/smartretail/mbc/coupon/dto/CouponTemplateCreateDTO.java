package com.smartretail.mbc.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "优惠券模板创建请求")
public class CouponTemplateCreateDTO {

    @NotBlank(message = "券编码不能为空")
    @Schema(description = "券编码(唯一)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String couponCode;

    @NotBlank(message = "券名称不能为空")
    @Schema(description = "券名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String couponName;

    @NotNull(message = "券类型不能为空")
    @Schema(description = "券类型：1满减 2兑换", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer couponType;

    @Schema(description = "发放总量：-1不限")
    private Integer totalAmount;

    @Schema(description = "满减门槛金额(满减必填)")
    private BigDecimal fullAmount;

    @Schema(description = "减免金额(满减必填)")
    private BigDecimal reduceAmount;

    @Schema(description = "兑换商品(兑换必填)")
    private String exchangeItem;

    @NotNull(message = "有效期类型不能为空")
    @Schema(description = "有效期类型：1固定 2领取N天", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer validType;

    @Schema(description = "有效期开始(validType=1必填)")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束(validType=1必填)")
    private LocalDateTime validEnd;

    @Schema(description = "有效天数(validType=2必填)")
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

    @Schema(description = "是否可叠加：1是 0否")
    private Integer stackable;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "关联活动ID")
    private Long activityId;
}
