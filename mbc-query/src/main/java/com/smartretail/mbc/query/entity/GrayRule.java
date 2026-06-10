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
@TableName("t_gray_rule")
public class GrayRule extends BaseEntity {

    private String grayCode;

    private String grayName;

    private Long activityId;

    private Integer grayType;

    private String grayConfig;

    private String ruleContent;

    private String originalRuleContent;

    private Integer grayRatio;

    private Integer status;

    private LocalDateTime startGrayTime;

    private LocalDateTime fullReleaseTime;

    private LocalDateTime rollbackTime;

    private String operator;

    private String description;
}
