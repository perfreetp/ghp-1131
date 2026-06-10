package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会员合并记录展示VO")
public class MergeLogVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "合并单号")
    private String mergeNo;

    @Schema(description = "被合并会员ID")
    private Long sourceMemberId;

    @Schema(description = "目标会员ID")
    private Long targetMemberId;

    @Schema(description = "被合并方手机号")
    private String sourcePhone;

    @Schema(description = "被合并方姓名")
    private String sourceName;

    @Schema(description = "目标方手机号")
    private String targetPhone;

    @Schema(description = "目标方姓名")
    private String targetName;

    @Schema(description = "合并迁移的积分")
    private Integer mergedPoints;

    @Schema(description = "合并迁移的成长值")
    private Integer mergedGrowth;

    @Schema(description = "合并迁移的优惠券数量")
    private Integer mergedCoupons;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "合并原因")
    private String reason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "格式化的创建时间字符串")
    private String createTimeFmt;
}
