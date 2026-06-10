package com.smartretail.mbc.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.query.entity.ActivityBudgetLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityBudgetLogMapper extends BaseMapper<ActivityBudgetLog> {

    IPage<ActivityBudgetLog> selectByActivityId(@Param("activityId") Long activityId, Page<ActivityBudgetLog> page);
}
