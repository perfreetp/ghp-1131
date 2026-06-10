package com.smartretail.mbc.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会员合并记录查询条件")
public class MemberMergeLogQueryDTO {

    @Schema(description = "被合并方手机号")
    private String sourcePhone;

    @Schema(description = "目标方手机号")
    private String targetPhone;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认20")
    private Integer pageSize = 20;
}
