package com.smartretail.mbc.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_member_merge_log")
public class MemberMergeLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String mergeNo;

    private Long sourceMemberId;

    private Long targetMemberId;

    private Integer mergedPoints;

    private Integer mergedGrowth;

    private Integer mergedCoupons;

    private String operator;

    private String reason;

    private LocalDateTime createTime;
}
