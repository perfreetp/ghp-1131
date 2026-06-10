package com.smartretail.mbc.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "履约步骤VO")
public class FulfillmentItemVO {

    @Schema(description = "步骤序号")
    private Integer stepNo;

    @Schema(description = "步骤类型: 1权益锁定 2权益核销 3权益返还 4订单支付 5订单完成 6订单退款")
    private Integer stepType;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "状态: 0待处理 1处理中 2已完成 3已失败")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "涉及金额")
    private BigDecimal amount;

    @Schema(description = "涉及积分")
    private Integer points;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    @Schema(description = "备注")
    private String remark;
}
