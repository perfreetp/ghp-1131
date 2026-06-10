package com.smartretail.mbc.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "成长值变更结果VO")
public class GrowthResultVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "变更前等级")
    private Integer beforeLevel;

    @Schema(description = "变更后等级")
    private Integer afterLevel;

    @Schema(description = "是否升级")
    private Boolean levelUp;

    @Schema(description = "成长值变动量")
    private Integer growthChange;

    @Schema(description = "当前成长值")
    private Integer currentGrowth;
}
