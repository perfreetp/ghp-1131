package com.smartretail.mbc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "风控检查入参")
public class RiskCheckDTO {

    @Schema(description = "风控场景，对应RiskSceneEnum")
    private Integer scene;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "扩展参数")
    private Map<String, Object> extra;
}
