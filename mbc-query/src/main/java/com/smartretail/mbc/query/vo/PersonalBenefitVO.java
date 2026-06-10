package com.smartretail.mbc.query.vo;

import com.smartretail.mbc.coupon.vo.CouponInstanceVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "个人权益清单总览")
public class PersonalBenefitVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "积分信息")
    private PointSummary pointInfo;

    @Schema(description = "券汇总")
    private CouponSummary couponSummary;

    @Schema(description = "可用券列表")
    private List<CouponInstanceVO> couponList;

    @Schema(description = "当前等级权益文字列表")
    private List<String> levelBenefits;

    @Schema(description = "近30天消费次数")
    private Long recentConsumeCount;

    @Schema(description = "累计消费金额")
    private BigDecimal totalConsumeAmount;

    @Data
    @Schema(description = "积分汇总")
    public static class PointSummary {

        @Schema(description = "当前可用积分")
        private Integer currentPoints;

        @Schema(description = "冻结积分")
        private Integer frozenPoints;

        @Schema(description = "累计获得")
        private Integer totalEarned;

        @Schema(description = "累计使用")
        private Integer totalUsed;

        @Schema(description = "30天内过期积分")
        private Integer expiringIn30Days;
    }

    @Data
    @Schema(description = "券汇总")
    public static class CouponSummary {

        @Schema(description = "可用券总数")
        private Integer totalAvailable;

        @Schema(description = "7天内过期数量")
        private Integer expiringIn7Days;

        @Schema(description = "30天内过期数量")
        private Integer expiringIn30Days;

        @Schema(description = "本月使用数量")
        private Integer usedThisMonth;
    }
}
