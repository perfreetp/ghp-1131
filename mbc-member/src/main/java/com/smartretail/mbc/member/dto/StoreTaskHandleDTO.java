package com.smartretail.mbc.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreTaskHandleDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "处理人不能为空")
    private String handler;

    private String handleResult;

    private Integer action;
}
