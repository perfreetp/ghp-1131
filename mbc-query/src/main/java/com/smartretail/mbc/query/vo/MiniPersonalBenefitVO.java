package com.smartretail.mbc.query.vo;

import com.smartretail.mbc.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "小程序个人中心权益总览")
public class MiniPersonalBenefitVO {

    @Schema(description = "会员卡信息")
    private MiniMemberCardVO memberCard;

    @Schema(description = "积分信息")
    private MiniPointVO pointInfo;

    @Schema(description = "生日权益信息")
    private MiniBirthdayVO birthdayInfo;

    @Schema(description = "等级权益列表")
    private List<LevelBenefitItemVO> levelBenefits;

    @Schema(description = "过期提醒汇总")
    private List<ExpireReminderVO> expireReminders;

    @Schema(description = "优惠券分页数据")
    private PageResult<MiniCouponVO> couponPage;

    @Schema(description = "消费统计")
    private MiniConsumeStatsVO consumeStats;
}
