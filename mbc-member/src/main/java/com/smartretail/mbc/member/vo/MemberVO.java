package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "会员详情展示VO")
public class MemberVO {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "会员码")
    private String memberCode;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别：1男 2女 0未知")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "等级图标")
    private String levelIcon;

    @Schema(description = "等级权益描述")
    private String benefitDesc;

    @Schema(description = "成长值")
    private Integer growthValue;

    @Schema(description = "当前积分")
    private Integer currentPoints;

    @Schema(description = "累计积分")
    private Integer totalPoints;

    @Schema(description = "注册来源")
    private String registerSource;

    @Schema(description = "状态：1正常 0禁用")
    private Integer status;

    @Schema(description = "合并到的会员ID")
    private Long mergedTo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
