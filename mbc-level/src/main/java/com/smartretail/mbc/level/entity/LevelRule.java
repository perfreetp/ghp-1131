package com.smartretail.mbc.level.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_level_rule")
public class LevelRule extends BaseEntity {

    private Integer levelCode;

    private String levelName;

    private Integer growthThreshold;

    private BigDecimal growthRatio;

    private BigDecimal pointRatio;

    private BigDecimal discountRate;

    private Integer birthdayPoints;

    private Long birthdayCouponId;

    private String benefitDesc;

    private String icon;

    private Integer status;
}
