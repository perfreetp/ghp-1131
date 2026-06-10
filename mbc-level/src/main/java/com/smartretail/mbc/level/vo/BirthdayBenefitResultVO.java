package com.smartretail.mbc.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "生日权益发放结果VO")
public class BirthdayBenefitResultVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "赠送积分数")
    private Integer grantedPoints;

    @Schema(description = "赠送的优惠券ID列表")
    private List<Long> grantedCouponIds;

    @Schema(description = "结果消息")
    private String message;
}
