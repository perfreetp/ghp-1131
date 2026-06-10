package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员身份识别请求")
public class MemberIdentityDTO {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "会员码")
    private String memberCode;
}
