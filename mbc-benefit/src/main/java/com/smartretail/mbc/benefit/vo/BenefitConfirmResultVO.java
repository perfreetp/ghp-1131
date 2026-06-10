package com.smartretail.mbc.benefit.vo;

import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "确认核销结果")
public class BenefitConfirmResultVO {

    @Schema(description = "锁定编号")
    private String useNo;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "权益抵扣总金额")
    private BigDecimal totalBenefitValue;

    @Schema(description = "是否确认成功")
    private Boolean confirmed;

    @Schema(description = "核销明细列表")
    private List<BenefitUseLog> detailList;

    @Schema(description = "是否为幂等返回")
    private Boolean idempotent;

    @Schema(description = "本次处理唯一ID")
    private String requestId;

    @Schema(description = "处理状态 1处理中 2已完成 3失败")
    private Integer processStatus;
}
