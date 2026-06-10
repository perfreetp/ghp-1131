package com.smartretail.mbc.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "消息查询请求")
public class MessageQueryDTO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "消息类型")
    private Integer msgType;

    @Schema(description = "发送状态 0待发1成功2失败3已读")
    private Integer sendStatus;

    @Schema(description = "推送渠道 INNER/SMS/WECHAT/APP_PUSH")
    private String channel;

    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认20")
    private Integer pageSize = 20;
}
