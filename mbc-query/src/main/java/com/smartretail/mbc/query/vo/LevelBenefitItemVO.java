package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "等级权益项")
public class LevelBenefitItemVO {

    @Schema(description = "权益标题")
    private String title;

    @Schema(description = "权益描述")
    private String desc;

    @Schema(description = "权益图标")
    private String icon;

    @Schema(description = "是否已达成")
    private Boolean achieved;

    @Schema(description = "所需等级编码")
    private Integer levelRequired;
}
