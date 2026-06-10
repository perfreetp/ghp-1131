package com.smartretail.mbc.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_coupon_template")
public class CouponTemplate extends BaseEntity {

    private String couponCode;

    private String couponName;

    private Integer couponType;

    private Integer totalAmount;

    private Integer receivedCount;

    private Integer usedCount;

    private BigDecimal fullAmount;

    private BigDecimal reduceAmount;

    private String exchangeItem;

    private Integer validType;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private Integer validDays;

    private Integer minLevel;

    private Integer dailyLimit;

    private Integer totalLimit;

    private String applyScenes;

    private String excludeItems;

    private Integer stackable;

    private String description;

    private Integer status;

    private Long activityId;
}
