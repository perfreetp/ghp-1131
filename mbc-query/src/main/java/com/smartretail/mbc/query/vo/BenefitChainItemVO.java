package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "权益链路步骤VO")
public class BenefitChainItemVO {

    @Schema(description = "步骤序号")
    private Integer stepNo;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "使用编号")
    private String useNo;

    @Schema(description = "权益类型")
    private Integer benefitType;

    @Schema(description = "权益类型名称")
    private String benefitTypeName;

    @Schema(description = "权益价值")
    private BigDecimal benefitValue;

    @Schema(description = "使用积分")
    private Integer usedPoints;

    @Schema(description = "使用状态")
    private Integer useStatus;

    @Schema(description = "使用状态名称")
    private String useStatusName;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "POS编码")
    private String posCode;
}
