package com.smartretail.mbc.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "批量发送消息请求")
public class MessageBatchDTO {

    @NotEmpty(message = "会员ID列表不能为空")
    @Schema(description = "会员ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberIds;

    @NotNull(message = "消息类型不能为空")
    @Schema(description = "消息类型 1-5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer msgType;

    @NotBlank(message = "消息标题不能为空")
    @Schema(description = "消息标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String msgTitle;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String msgContent;

    @Schema(description = "推送渠道 INNER/SMS/WECHAT/APP_PUSH", defaultValue = "INNER")
    private String channel = "INNER";

    @Schema(description = "业务ID")
    private Long bizId;

    @Schema(description = "业务扩展数据(JSON)")
    private Map<String, Object> bizData;
}
