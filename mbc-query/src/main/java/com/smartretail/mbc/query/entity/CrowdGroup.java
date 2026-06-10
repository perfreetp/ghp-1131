package com.smartretail.mbc.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_crowd_group")
public class CrowdGroup extends BaseEntity {

    private String crowdCode;

    private String crowdName;

    private Integer crowdType;

    private String ruleConfig;

    private Integer estimatedCount;

    private Integer actualCount;

    private Integer status;

    private LocalDateTime refreshTime;

    private String description;
}
