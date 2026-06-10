package com.smartretail.mbc.level.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_growth_log")
public class GrowthLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Integer changeValue;

    private Integer beforeValue;

    private Integer afterValue;

    private Integer beforeLevel;

    private Integer afterLevel;

    private Integer sourceType;

    private String sourceId;

    private String remark;

    private LocalDateTime createTime;

    private String createBy;
}
