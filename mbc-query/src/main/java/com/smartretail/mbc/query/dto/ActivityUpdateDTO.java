package com.smartretail.mbc.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "修改活动请求")
public class ActivityUpdateDTO {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @Schema(description = "活动编码")
    private String activityCode;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "活动类型：1发券 2积分 3等级 4生日 5积分翻倍")
    private Integer activityType;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "目标会员等级：0=不限")
    private Integer targetLevel;

    @Schema(description = "发券活动：关联券模板ID列表")
    private List<Long> couponTemplateIds;

    @Schema(description = "积分翻倍活动：倍率 如2.0=双倍")
    private BigDecimal pointMultiplier;

    @Schema(description = "积分活动：每单送积分")
    private Integer pointPerOrder;

    @Schema(description = "生日礼活动：送积分")
    private Integer birthdayPoints;

    @Schema(description = "生日礼活动：送券ID列表")
    private List<Long> birthdayCouponIds;

    @Schema(description = "等级活动：成长值倍率")
    private BigDecimal growthMultiplier;

    @Schema(description = "预算积分")
    private Integer budgetPoints;

    @Schema(description = "预算券数量")
    private Integer budgetCoupons;

    @Schema(description = "适用场景")
    private String applyScenes;

    @Schema(description = "补充规则JSON")
    private String ruleConfig;

    @Schema(description = "活动描述")
    private String description;

    @Schema(description = "活动状态：0草稿 1进行中 2已结束 3已取消")
    private Integer status;

    @Schema(description = "关联人群组ID")
    private Long crowdGroupId;
}
