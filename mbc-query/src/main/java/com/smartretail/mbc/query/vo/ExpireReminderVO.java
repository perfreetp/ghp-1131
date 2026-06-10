package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "过期提醒信息")
public class ExpireReminderVO {

    @Schema(description = "类型：1券 2积分")
    private Integer type;

    @Schema(description = "提醒标题")
    private String title;

    @Schema(description = "提醒内容")
    private String content;

    @Schema(description = "过期数量")
    private Integer count;

    @Schema(description = "过期日期范围描述")
    private String expireDateRange;
}
