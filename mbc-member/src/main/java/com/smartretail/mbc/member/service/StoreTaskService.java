package com.smartretail.mbc.member.service;

import com.smartretail.mbc.member.dto.StoreTaskHandleDTO;
import com.smartretail.mbc.member.dto.StoreTaskQueryDTO;
import com.smartretail.mbc.member.vo.StoreTaskBoardVO;

import java.time.LocalDate;

public interface StoreTaskService {

    StoreTaskBoardVO getStoreTaskBoard(StoreTaskQueryDTO dto);

    void handleTask(StoreTaskHandleDTO dto);

    void generateDailyTasks(String storeCode, LocalDate date);
}
