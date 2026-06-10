package com.smartretail.mbc.query.vo;

import com.smartretail.mbc.query.entity.GrayRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "灰度规则详情VO")
public class GrayRuleVO extends GrayRule {

    @Schema(description = "灰度类型名称")
    private String grayTypeName;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "灰度效果")
    private GrayEffectVO effect;
}
