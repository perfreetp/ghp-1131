package com.smartretail.mbc.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_store_task")
public class StoreTask extends BaseEntity {

    private String taskNo;

    private String storeCode;

    private String storeName;

    private Integer taskType;

    private Integer bizType;

    private String bizId;

    private String title;

    private String description;

    private Integer priority;

    private Integer status;

    private String handler;

    private LocalDateTime handleTime;

    private String handleResult;

    private String source;
}
