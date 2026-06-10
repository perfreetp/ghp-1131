package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "会员更新请求")
public class MemberUpdateDTO {

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

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
}
