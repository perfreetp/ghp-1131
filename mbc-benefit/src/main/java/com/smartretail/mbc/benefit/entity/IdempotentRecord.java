package com.smartretail.mbc.benefit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_idempotent_record")
public class IdempotentRecord extends BaseEntity {

    private String businessNo;

    private Integer businessType;

    private Integer processStatus;

    private String requestId;

    private String requestParam;

    private Integer resultCode;

    private String resultMsg;

    private Integer retryCount;

    private String operator;

    private Integer operatorType;

    private LocalDateTime operateTime;

    private LocalDateTime nextRetryTime;

    private String remark;
}
