package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "会员注册请求")
public class MemberRegisterDTO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别：1男 2女 0未知")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "注册来源")
    private String registerSource;
}
