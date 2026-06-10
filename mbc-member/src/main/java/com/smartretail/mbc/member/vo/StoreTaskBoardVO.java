package com.smartretail.mbc.member.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class StoreTaskBoardVO {

    private String storeCode;

    private String storeName;

    private Integer totalPending;

    private Integer highPriorityCount;

    private List<TaskStatVO> taskStats;

    private IPage<StoreTaskVO> tasks;
}
