package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "人群成员VO")
public class CrowdMemberVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员编码")
    private String memberCode;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "当前积分")
    private Integer currentPoints;

    @Schema(description = "入群时间")
    private LocalDateTime matchTime;

    @Schema(description = "匹配原因")
    private String matchReason;
}
