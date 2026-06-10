package com.smartretail.mbc.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "券实例VO")
public class CouponInstanceVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "券实例编号")
    private String instanceNo;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "券状态")
    private Integer couponStatus;

    @Schema(description = "有效期开始")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束")
    private LocalDateTime validEnd;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "使用订单号")
    private String usedOrderNo;

    @Schema(description = "锁定时间")
    private LocalDateTime lockedTime;

    @Schema(description = "锁定订单号")
    private String lockOrderNo;

    @Schema(description = "领取来源")
    private String receiveSource;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "券模板信息")
    private CouponTemplateVO template;

    @Schema(description = "进度条文本(如:已过期前3天)")
    private String progressBarText;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
