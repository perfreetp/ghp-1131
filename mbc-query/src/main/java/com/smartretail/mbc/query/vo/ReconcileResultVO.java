package com.smartretail.mbc.query.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "对账结果VO")
public class ReconcileResultVO {

    @Schema(description = "对账汇总列表")
    private List<ReconcileSummaryVO> summaryList;

    @Schema(description = "对账明细分页")
    private IPage<ReconcileDetailVO> details;
}
