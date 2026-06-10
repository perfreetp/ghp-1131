package com.smartretail.mbc.point.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_point_log")
public class PointLog extends BaseEntity {

    private Long memberId;

    private Integer pointType;

    private Integer changePoints;

    private Integer beforePoints;

    private Integer afterPoints;

    private Integer frozenPoints;

    private Integer sourceType;

    private String sourceId;

    private LocalDateTime expireTime;

    private String remark;
}
