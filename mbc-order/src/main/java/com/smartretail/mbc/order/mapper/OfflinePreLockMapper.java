package com.smartretail.mbc.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.order.entity.OfflinePreLock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OfflinePreLockMapper extends BaseMapper<OfflinePreLock> {

    OfflinePreLock selectByOfflineLockNo(@Param("offlineLockNo") String offlineLockNo);

    IPage<OfflinePreLock> selectByStoreAndStatus(@Param("storeCode") String storeCode,
                                                  @Param("syncStatus") Integer syncStatus,
                                                  Page<OfflinePreLock> page);
}
