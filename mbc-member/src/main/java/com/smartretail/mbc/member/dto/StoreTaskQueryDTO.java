package com.smartretail.mbc.member.dto;

import lombok.Data;

@Data
public class StoreTaskQueryDTO {

    private String storeCode;

    private Integer taskType;

    private Integer status;

    private Integer priority;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
