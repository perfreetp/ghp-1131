package com.smartretail.mbc.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "离线预锁上报DTO")
public class OfflinePreLockDTO {

    @Schema(description = "离线锁号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "离线锁号不能为空")
    private String offlineLockNo;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "收银员")
    private String cashier;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "正式订单号(可空，收银端可能没生成)")
    private String orderNo;

    @Schema(description = "权益类型 1券 2积分 3组合")
    private Integer benefitType;

    @Schema(description = "使用的券ID列表")
    private List<Long> couponIds;

    @Schema(description = "使用积分数")
    private Integer usedPoints;

    @Schema(description = "权益总金额")
    private BigDecimal totalBenefitValue;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "预锁时间")
    private LocalDateTime preLockTime;

    @Schema(description = "商品明细")
    private List<OrderItemDTO> items;

    @Schema(description = "离线风险预判结果JSON")
    private String riskCheckResult;
}
