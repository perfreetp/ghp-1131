package com.smartretail.mbc.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "未读计数VO")
public class UnreadCountVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "未读总数")
    private Integer totalUnread;

    @Schema(description = "按消息类型分组的未读数")
    private Map<Integer, Integer> countsByType;
}
