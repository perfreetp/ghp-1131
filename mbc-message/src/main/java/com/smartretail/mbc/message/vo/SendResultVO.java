package com.smartretail.mbc.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "发送结果VO")
public class SendResultVO {

    @Schema(description = "消息编号")
    private String msgNo;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
}
