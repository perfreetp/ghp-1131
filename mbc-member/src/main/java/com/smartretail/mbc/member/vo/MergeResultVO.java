package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员合并结果VO")
public class MergeResultVO {

    @Schema(description = "合并单号")
    private String mergeNo;

    @Schema(description = "被合并会员ID")
    private Long sourceMemberId;

    @Schema(description = "目标会员ID")
    private Long targetMemberId;

    @Schema(description = "合并迁移的积分")
    private Integer mergedPoints;

    @Schema(description = "合并迁移的成长值")
    private Integer mergedGrowth;

    @Schema(description = "合并迁移的优惠券数量")
    private Integer mergedCoupons;
}
