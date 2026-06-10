package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "权益处理链路VO")
public class BenefitChainVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "订单状态名称")
    private String orderStatusName;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员名称")
    private String memberName;

    @Schema(description = "锁定时间")
    private LocalDateTime lockTime;

    @Schema(description = "核销时间")
    private LocalDateTime confirmTime;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "权益处理步骤")
    private List<BenefitChainItemVO> benefitLogs;

    @Schema(description = "关联的幂等记录")
    private List<IdempotentRecordVO> idempotentRecords;
}
