package com.smartretail.mbc.message.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.common.enums.MessageTypeEnum;
import com.smartretail.mbc.message.entity.MessageLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息展示VO")
public class MessageVO extends MessageLog {

    @Schema(description = "消息类型名称")
    private String msgTypeName;

    @Schema(description = "推送渠道名称")
    private String channelName;

    @Schema(description = "发送状态名称")
    private String sendStatusName;

    @Schema(description = "业务扩展数据Map")
    private Map<String, Object> bizDataMap;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String getMsgTypeName() {
        if (getMsgType() != null) {
            for (MessageTypeEnum typeEnum : MessageTypeEnum.values()) {
                if (typeEnum.getCode().equals(getMsgType())) {
                    return typeEnum.getName();
                }
            }
        }
        return msgTypeName;
    }

    public String getChannelName() {
        if (getChannel() != null) {
            switch (getChannel()) {
                case "INNER":
                    return "站内信";
                case "SMS":
                    return "短信";
                case "WECHAT":
                    return "微信";
                case "APP_PUSH":
                    return "APP推送";
                default:
                    return getChannel();
            }
        }
        return channelName;
    }

    public String getSendStatusName() {
        if (getSendStatus() != null) {
            switch (getSendStatus()) {
                case 0:
                    return "待发送";
                case 1:
                    return "发送成功";
                case 2:
                    return "发送失败";
                case 3:
                    return "已读";
                default:
                    return "未知";
            }
        }
        return sendStatusName;
    }

    public Map<String, Object> getBizDataMap() {
        if (getBizData() != null && !getBizData().isEmpty()) {
            try {
                return OBJECT_MAPPER.readValue(getBizData(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("解析bizData JSON失败: {}", getBizData(), e);
            }
        }
        return new HashMap<>();
    }
}
