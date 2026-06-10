package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员简单信息VO")
public class MemberSimpleVO {

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

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;
}
