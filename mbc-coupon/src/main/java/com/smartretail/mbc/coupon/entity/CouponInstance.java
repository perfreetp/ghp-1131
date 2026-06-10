package com.smartretail.mbc.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_coupon_instance")
public class CouponInstance extends BaseEntity {

    private String instanceNo;

    private Long templateId;

    private Long memberId;

    private Integer couponStatus;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private LocalDateTime usedTime;

    private String usedOrderNo;

    private LocalDateTime lockedTime;

    private String lockOrderNo;

    private String receiveSource;

    private LocalDateTime receiveTime;

    private Long sourceId;

    private String remark;
}
