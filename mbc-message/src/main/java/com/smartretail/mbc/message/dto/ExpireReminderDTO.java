package com.smartretail.mbc.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "批量推送到期提醒请求")
public class ExpireReminderDTO {

    @NotNull(message = "提醒类型不能为空")
    @Schema(description = "提醒类型 1券 2积分", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reminderType;

    @Schema(description = "到期天数，默认3天", defaultValue = "3")
    private Integer days = 3;
}
