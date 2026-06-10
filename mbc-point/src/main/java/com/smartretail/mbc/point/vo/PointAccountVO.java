package com.smartretail.mbc.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@Schema(description = "积分账户概览")
public class PointAccountVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "可用积分")
    private Integer currentPoints;

    @Schema(description = "冻结积分")
    private Integer frozenPoints;

    @Schema(description = "30天内即将过期积分")
    private Integer expiringSoonPoints;

    @Schema(description = "过期时间分布（日期->积分数）")
    private Map<LocalDate, Integer> expireTimeMap;

    @Schema(description = "累计获得积分")
    private Integer totalEarned;

    @Schema(description = "累计使用积分")
    private Integer totalUsed;
}
