package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_reconcile_record")
public class ReconcileRecord extends BaseEntity {

    private String useNo;

    private String orderNo;

    private Integer benefitType;

    private BigDecimal benefitValue;

    private BigDecimal posPayAmount;

    private LocalDateTime posPayTime;

    private Integer reconcileStatus;

    private String reconcileDiff;

    private LocalDate reconcileDate;

    private LocalDateTime reconcileTime;
}
