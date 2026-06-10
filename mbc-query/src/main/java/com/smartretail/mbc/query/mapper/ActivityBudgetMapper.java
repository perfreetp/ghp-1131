package com.smartretail.mbc.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.query.entity.ActivityBudget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityBudgetMapper extends BaseMapper<ActivityBudget> {

    List<ActivityBudget> selectByActivityId(@Param("activityId") Long activityId);

    ActivityBudget selectByActivityAndStore(@Param("activityId") Long activityId, @Param("storeCode") String storeCode);
}
