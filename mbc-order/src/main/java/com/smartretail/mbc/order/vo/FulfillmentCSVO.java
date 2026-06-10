package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "客服处理信息VO")
public class FulfillmentCSVO {

    @Schema(description = "是否有客服介入")
    private Boolean hasCsIntervention;

    @Schema(description = "客服人员")
    private String csStaff;

    @Schema(description = "客服处理时间")
    private LocalDateTime csTime;

    @Schema(description = "客服动作: 重放/标记失败/风险处置")
    private String csAction;

    @Schema(description = "处理结果")
    private String csResult;

    @Schema(description = "备注")
    private String csRemark;
}
