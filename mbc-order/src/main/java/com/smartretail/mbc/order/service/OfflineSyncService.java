package com.smartretail.mbc.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.order.dto.OfflinePreLockDTO;
import com.smartretail.mbc.order.vo.OfflinePreLockVO;
import com.smartretail.mbc.order.vo.OfflineSyncResultVO;

public interface OfflineSyncService {

    OfflineSyncResultVO syncOfflinePreLock(OfflinePreLockDTO dto);

    IPage<OfflinePreLockVO> queryOfflineRecords(String storeCode, Integer syncStatus, Integer pageNum, Integer pageSize);

    void retryFailedOfflineLocks(String storeCode);
}
