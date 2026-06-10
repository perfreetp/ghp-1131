package com.smartretail.mbc.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "积分流水详情")
public class PointLogVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "积分类型编码：1增加 2扣减 3冻结 4解冻")
    private Integer pointType;

    @Schema(description = "积分类型名称")
    private String pointTypeName;

    @Schema(description = "变更积分数量")
    private Integer changePoints;

    @Schema(description = "变更前积分")
    private Integer beforePoints;

    @Schema(description = "变更后积分")
    private Integer afterPoints;

    @Schema(description = "冻结积分数量（仅冻结/解冻时有值）")
    private Integer frozenPoints;

    @Schema(description = "来源类型编码")
    private Integer sourceType;

    @Schema(description = "来源类型名称")
    private String sourceTypeName;

    @Schema(description = "来源ID")
    private String sourceId;

    @Schema(description = "过期时间（仅增加时有值）")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String createBy;
}
