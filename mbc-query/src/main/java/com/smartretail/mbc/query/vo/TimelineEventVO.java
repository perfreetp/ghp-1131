package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "时间线事件VO")
public class TimelineEventVO {

    @Schema(description = "唯一事件ID")
    private String eventId;

    @Schema(description = "事件类型")
    private Integer eventType;

    @Schema(description = "事件类型名称")
    private String eventTypeName;

    @Schema(description = "事件简短标签")
    private String eventTag;

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "发生时间")
    private LocalDateTime eventTime;

    @Schema(description = "业务类型: 1订单 2券 3积分 4等级 5合并 6消息")
    private Integer bizType;

    @Schema(description = "业务ID: 订单号/券ID/积分批次号等")
    private String bizId;

    @Schema(description = "涉及金额")
    private BigDecimal amount;

    @Schema(description = "涉及积分数")
    private Integer points;

    @Schema(description = "详情扩展字段")
    private Map<String, Object> detail;

    @Schema(description = "关联操作人/渠道")
    private String relatedStaff;

    @Schema(description = "方向: 1增加 -1减少 0中性")
    private Integer direction;
}
