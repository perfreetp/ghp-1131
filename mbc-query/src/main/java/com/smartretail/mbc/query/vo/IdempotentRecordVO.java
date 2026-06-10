package com.smartretail.mbc.query.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "幂等记录VO")
public class IdempotentRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "业务号")
    private String businessNo;

    @Schema(description = "业务类型")
    private Integer businessType;

    @Schema(description = "业务类型名称")
    private String businessTypeName;

    @Schema(description = "处理状态")
    private Integer processStatus;

    @Schema(description = "处理状态名称")
    private String processStatusName;

    @Schema(description = "请求唯一ID")
    private String requestId;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作类型")
    private Integer operatorType;

    @Schema(description = "操作类型名称")
    private String operatorTypeName;

    @Schema(description = "最近操作时间")
    private LocalDateTime operateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否可重放")
    private Boolean canReplay;

    @Schema(description = "是否可标记失败")
    private Boolean canMarkFail;
}
