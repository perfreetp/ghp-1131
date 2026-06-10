package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "异常波动门店VO")
public class AbnormalStoreVO {

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "异常类型: 1核销突增 2退款突增 3风险突增 4预算超支 5核销率下降")
    private Integer abnormalType;

    @Schema(description = "异常类型名称")
    private String abnormalTypeName;

    @Schema(description = "当前值")
    private String currentValue;

    @Schema(description = "上期值")
    private String lastValue;

    @Schema(description = "变化率")
    private BigDecimal changeRate;

    @Schema(description = "严重程度: 1轻微 2中等 3严重")
    private Integer severity;

    @Schema(description = "检测时间")
    private LocalDateTime detectedTime;
}
