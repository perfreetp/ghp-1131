package com.smartretail.mbc.benefit.vo;

import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "核销记录展示VO")
public class BenefitUseVO extends BenefitUseLog {

    @Schema(description = "权益类型名称")
    private String benefitTypeName;

    @Schema(description = "使用状态名称")
    private String useStatusName;

    @Schema(description = "券名称(券类型时显示)")
    private String couponInfo;

    @Schema(description = "是否为幂等返回")
    private Boolean idempotent;

    @Schema(description = "本次处理唯一ID")
    private String requestId;

    @Schema(description = "处理状态 1处理中 2已完成 3失败")
    private Integer processStatus;

    public Long getId() {
        return super.getId();
    }

    public String getUseNo() {
        return super.getUseNo();
    }

    public Long getMemberId() {
        return super.getMemberId();
    }

    public Integer getBenefitType() {
        return super.getBenefitType();
    }

    public Long getBenefitId() {
        return super.getBenefitId();
    }

    public Integer getUseStatus() {
        return super.getUseStatus();
    }

    public String getOrderNo() {
        return super.getOrderNo();
    }

    public BigDecimal getOrderAmount() {
        return super.getOrderAmount();
    }

    public BigDecimal getBenefitValue() {
        return super.getBenefitValue();
    }

    public Integer getUsedPoints() {
        return super.getUsedPoints();
    }

    public String getStoreCode() {
        return super.getStoreCode();
    }

    public String getPosCode() {
        return super.getPosCode();
    }

    public String getOperator() {
        return super.getOperator();
    }

    public LocalDateTime getLockTime() {
        return super.getLockTime();
    }

    public LocalDateTime getConfirmTime() {
        return super.getConfirmTime();
    }

    public LocalDateTime getReturnTime() {
        return super.getReturnTime();
    }

    public String getReturnReason() {
        return super.getReturnReason();
    }

    public String getRemark() {
        return super.getRemark();
    }
}
