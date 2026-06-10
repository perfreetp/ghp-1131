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
@TableName("t_offline_pre_lock")
public class OfflinePreLock extends BaseEntity {

    private String offlineLockNo;

    private String storeCode;

    private String posCode;

    private String cashier;

    private Long memberId;

    private String orderNo;

    private String useNo;

    private Integer benefitType;

    private String couponIds;

    private Integer usedPoints;

    private BigDecimal totalBenefitValue;

    private BigDecimal orderAmount;

    private LocalDateTime preLockTime;

    private LocalDateTime syncTime;

    private Integer syncStatus;

    private Integer syncRetryCount;

    private String syncErrorMsg;

    private Boolean isIdempotent;

    private String operator;

    private String remark;
}
