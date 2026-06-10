package com.smartretail.mbc.member.vo;

import lombok.Data;

@Data
public class TaskStatVO {

    private Integer taskType;

    private String taskTypeName;

    private Integer pendingCount;

    private Integer processingCount;

    private Integer doneCount;

    private Integer totalCount;
}
