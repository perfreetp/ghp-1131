package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "小程序积分信息")
public class MiniPointVO {

    @Schema(description = "当前可用积分")
    private Integer currentPoints;

    @Schema(description = "冻结积分")
    private Integer frozenPoints;

    @Schema(description = "7天内即将过期积分")
    private Integer expiringIn7Days;

    @Schema(description = "30天内即将过期积分")
    private Integer expiringIn30Days;

    @Schema(description = "30天内是否有过期积分提醒")
    private Boolean expireSoonFlag;
}
