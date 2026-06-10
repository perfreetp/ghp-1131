package com.smartretail.mbc.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_message_log")
public class MessageLog extends BaseEntity {

    private String msgNo;

    private Long memberId;

    private Integer msgType;

    private String msgTitle;

    private String msgContent;

    private String channel;

    private String target;

    private Integer sendStatus;

    private Integer retryCount;

    private String failReason;

    private LocalDateTime sendTime;

    private LocalDateTime readTime;

    private Long bizId;

    private String bizData;
}
