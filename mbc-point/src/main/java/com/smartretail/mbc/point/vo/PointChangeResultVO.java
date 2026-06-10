package com.smartretail.mbc.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "积分变更结果")
public class PointChangeResultVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "变更前可用积分")
    private Integer beforePoints;

    @Schema(description = "变更后可用积分")
    private Integer afterPoints;

    @Schema(description = "变更积分数量（正数）")
    private Integer changePoints;

    @Schema(description = "变更类型：ADD/SUBTRACT/FREEZE/UNFREEZE")
    private String changeType;

    @Schema(description = "流水记录ID")
    private Long logId;
}
