package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "小程序会员卡信息")
public class MiniMemberCardVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员编码")
    private String memberCode;

    @Schema(description = "会员姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "等级图标")
    private String levelIcon;

    @Schema(description = "成长值")
    private Integer growthValue;

    @Schema(description = "下一等级门槛")
    private Integer nextLevelThreshold;

    @Schema(description = "成长进度百分比")
    private BigDecimal growthProgress;

    @Schema(description = "当前等级权益描述")
    private String currentLevelBenefitDesc;

    @Schema(description = "下一等级权益描述")
    private String nextLevelBenefitDesc;

    @Schema(description = "累计消费金额")
    private BigDecimal totalSpentAmount;
}
