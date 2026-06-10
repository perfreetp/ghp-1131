package com.smartretail.mbc.order.entity;

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
@TableName("t_consume_order")
public class ConsumeOrder extends BaseEntity {

    private String orderNo;

    private Long memberId;

    private Integer orderType;

    private Integer orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal couponAmount;

    private BigDecimal pointAmount;

    private BigDecimal levelDiscount;

    private BigDecimal payAmount;

    private Integer earnedPoints;

    private Integer earnedGrowth;

    private Integer usedPoints;

    private String usedCouponIds;

    private String storeCode;

    private String storeName;

    private String posCode;

    private String cashier;

    private LocalDateTime payTime;

    private LocalDateTime completeTime;

    private LocalDateTime refundTime;

    private String refundNo;

    private BigDecimal refundAmount;

    private String channel;

    private String remark;
}
