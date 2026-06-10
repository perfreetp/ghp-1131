package com.smartretail.mbc.member.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreTaskVO {

    private Long id;

    private String taskNo;

    private Integer taskType;

    private String taskTypeName;

    private Integer bizType;

    private String bizTypeName;

    private String bizId;

    private String title;

    private String description;

    private Integer priority;

    private String priorityName;

    private Integer status;

    private String statusName;

    private String handler;

    private LocalDateTime handleTime;

    private String handleResult;

    private String source;

    private String sourceName;

    private LocalDateTime createTime;
}
