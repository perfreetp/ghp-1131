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
@TableName("t_crowd_member")
public class CrowdMember extends BaseEntity {

    private Long crowdId;

    private Long memberId;

    private LocalDateTime matchTime;

    private LocalDateTime expireTime;

    private Integer isActive;

    private String matchReason;
}
