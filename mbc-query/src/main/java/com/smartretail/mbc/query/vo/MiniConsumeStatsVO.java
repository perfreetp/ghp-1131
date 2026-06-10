package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "小程序消费统计")
public class MiniConsumeStatsVO {

    @Schema(description = "本月消费次数")
    private Integer monthCount;

    @Schema(description = "本月消费金额")
    private BigDecimal monthAmount;

    @Schema(description = "累计消费次数")
    private Integer totalCount;

    @Schema(description = "累计消费金额")
    private BigDecimal totalAmount;

    @Schema(description = "最近一次消费时间")
    private LocalDateTime lastConsumeTime;
}
