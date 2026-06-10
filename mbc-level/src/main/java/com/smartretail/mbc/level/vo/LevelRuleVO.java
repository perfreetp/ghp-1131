package com.smartretail.mbc.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "等级规则VO")
public class LevelRuleVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "成长值门槛")
    private Integer growthThreshold;

    @Schema(description = "成长值倍率")
    private BigDecimal growthRatio;

    @Schema(description = "积分倍率")
    private BigDecimal pointRatio;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "生日赠送积分")
    private Integer birthdayPoints;

    @Schema(description = "生日赠送优惠券ID")
    private Long birthdayCouponId;

    @Schema(description = "权益描述")
    private String benefitDesc;

    @Schema(description = "等级图标")
    private String icon;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "该等级会员数")
    private Integer memberCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
