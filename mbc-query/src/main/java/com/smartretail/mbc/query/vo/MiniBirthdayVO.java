package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "小程序生日权益信息")
public class MiniBirthdayVO {

    @Schema(description = "生日日期(MM-dd格式)")
    private String birthdayDate;

    @Schema(description = "距生日天数")
    private Integer daysUntilBirthday;

    @Schema(description = "今年生日礼状态：0未到/1已领/2已过期")
    private Integer thisYearBenefitStatus;

    @Schema(description = "已发放生日积分")
    private Integer grantedPoints;

    @Schema(description = "已发放生日券数量")
    private Integer grantedCouponCount;

    @Schema(description = "下次生日礼预览")
    private String nextBirthdayBenefitPreview;
}
