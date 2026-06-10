package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "会员合并预览详情VO")
public class MemberMergePreviewVO {

    @Schema(description = "被合并方会员信息")
    private MergeMemberInfoVO sourceMember;

    @Schema(description = "保留方会员信息")
    private MergeMemberInfoVO targetMember;

    @Schema(description = "差异信息汇总")
    private MergeDiffSummaryVO diffSummary;

    @Schema(description = "合并模拟结果")
    private MergeResultPreviewVO mergePreview;

    @Schema(description = "风险提示列表")
    private List<String> warnings;

    @Data
    @Schema(description = "合并预览会员信息")
    public static class MergeMemberInfoVO {

        @Schema(description = "会员ID")
        private Long memberId;

        @Schema(description = "会员码")
        private String memberCode;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "头像URL")
        private String avatar;

        @Schema(description = "生日")
        private LocalDate birthday;

        @Schema(description = "注册时间")
        private LocalDateTime registerTime;

        @Schema(description = "注册来源")
        private String registerSource;

        @Schema(description = "状态：1正常 0禁用")
        private Integer status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "等级编码")
        private Integer levelCode;

        @Schema(description = "等级名称")
        private String levelName;

        @Schema(description = "成长值")
        private Integer growthValue;

        @Schema(description = "当前可用积分")
        private Integer currentPoints;

        @Schema(description = "累计积分")
        private Integer totalPoints;

        @Schema(description = "优惠券总数量")
        private Integer totalCouponCount;

        @Schema(description = "可用优惠券数量")
        private Integer availableCouponCount;

        @Schema(description = "累计消费次数")
        private Integer totalConsumeCount;

        @Schema(description = "累计消费金额")
        private BigDecimal totalConsumeAmount;

        @Schema(description = "最后消费时间")
        private LocalDateTime lastConsumeTime;
    }

    @Data
    @Schema(description = "会员差异汇总")
    public static class MergeDiffSummaryVO {

        @Schema(description = "手机号是否不同")
        private Boolean phoneDiff;

        @Schema(description = "等级差异，格式：青铜→白银")
        private String levelDiff;

        @Schema(description = "被合并方可迁移积分数量")
        private Integer pointsDiff;

        @Schema(description = "可迁移优惠券数量")
        private Integer couponDiff;

        @Schema(description = "可迁移成长值")
        private Integer growthDiff;

        @Schema(description = "消费记录数差异")
        private Integer consumeDiff;

        @Schema(description = "重复信息提示，如：姓名相同/生日不同")
        private String duplicateInfo;
    }

    @Data
    @Schema(description = "合并结果预览")
    public static class MergeResultPreviewVO {

        @Schema(description = "合并后总积分")
        private Integer finalPoints;

        @Schema(description = "合并后总成长值")
        private Integer finalGrowth;

        @Schema(description = "合并后等级编码")
        private Integer finalLevelCode;

        @Schema(description = "合并后等级名称")
        private String finalLevelName;

        @Schema(description = "合并后总优惠券数")
        private Integer totalCouponsAfter;

        @Schema(description = "合并后总消费次数")
        private Integer totalConsumeCountAfter;

        @Schema(description = "合并后总消费金额")
        private BigDecimal totalConsumeAmountAfter;

        @Schema(description = "将迁移的积分")
        private Integer pointsToTransfer;

        @Schema(description = "将迁移的优惠券数")
        private Integer couponsToTransfer;

        @Schema(description = "将迁移的成长值")
        private Integer growthToTransfer;

        @Schema(description = "合并描述")
        private String description;
    }
}
