package com.smartretail.mbc.benefit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "锁定权益请求")
public class BenefitLockDTO {

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "权益类型 1优惠券 2积分抵扣 3等级折扣 4兑换权益", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "权益类型不能为空")
    private Integer benefitType;

    @Schema(description = "券ID列表(优惠券类型必填，其他类型填null)")
    private List<Long> benefitId;

    @Schema(description = "订单金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "使用积分数(积分抵扣类型可选)")
    private Integer usedPoints;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "操作员")
    private String operator;
}
