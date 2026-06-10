package com.smartretail.mbc.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_member")
public class Member extends BaseEntity {

    private String memberCode;

    private String phone;

    private String name;

    private String nickname;

    private Integer gender;

    private LocalDate birthday;

    private String avatar;

    private Integer levelCode;

    private Integer growthValue;

    private Integer currentPoints;

    private Integer totalPoints;

    private String registerSource;

    private Integer status;

    private Long mergedTo;

    private String remark;
}
