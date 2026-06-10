package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "离线预锁记录详情VO")
public class OfflinePreLockVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "离线锁号")
    private String offlineLockNo;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "收银员")
    private String cashier;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "正式订单号")
    private String orderNo;

    @Schema(description = "权益使用编号")
    private String useNo;

    @Schema(description = "权益类型 1券 2积分 3组合")
    private Integer benefitType;

    @Schema(description = "权益类型名称")
    private String benefitTypeName;

    @Schema(description = "使用的券ID列表JSON")
    private String couponIds;

    @Schema(description = "使用积分数")
    private Integer usedPoints;

    @Schema(description = "权益总金额")
    private BigDecimal totalBenefitValue;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "预锁时间")
    private LocalDateTime preLockTime;

    @Schema(description = "同步时间")
    private LocalDateTime syncTime;

    @Schema(description = "同步状态 0待同步 1同步中 2同步成功 3同步失败")
    private Integer syncStatus;

    @Schema(description = "同步状态名称")
    private String syncStatusName;

    @Schema(description = "重试次数")
    private Integer syncRetryCount;

    @Schema(description = "同步错误信息")
    private String syncErrorMsg;

    @Schema(description = "是否幂等返回")
    private Boolean isIdempotent;

    @Schema(description = "操作员")
    private String operator;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
