package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员查询条件")
public class MemberQueryDTO {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "会员码")
    private String memberCode;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "状态：1正常 0禁用")
    private Integer status;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10")
    private Integer pageSize = 10;
}
