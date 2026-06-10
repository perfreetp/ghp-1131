package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "对账汇总VO")
public class ReconcileSummaryVO {

    @Schema(description = "分组值")
    private String groupKey;

    @Schema(description = "分组显示名")
    private String groupLabel;

    @Schema(description = "核销笔数")
    private Integer totalConfirmCount;

    @Schema(description = "核销权益金额")
    private BigDecimal totalConfirmAmount;

    @Schema(description = "返还笔数")
    private Integer totalReturnCount;

    @Schema(description = "返还权益金额")
    private BigDecimal totalReturnAmount;

    @Schema(description = "锁定笔数")
    private Integer totalLockCount;

    @Schema(description = "异常重试笔数")
    private Integer totalRetryCount;

    @Schema(description = "匹配笔数")
    private Integer matchedCount;

    @Schema(description = "不匹配笔数")
    private Integer unmatchedCount;

    @Schema(description = "不匹配率")
    private BigDecimal unmatchedRate;
}
