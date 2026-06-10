package com.smartretail.mbc.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.point.entity.PointLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointLogMapper extends BaseMapper<PointLog> {

    List<PointLog> selectExpiringPoints(@Param("memberId") Long memberId, @Param("days") Integer days);

    Integer selectTotalByType(@Param("memberId") Long memberId, @Param("pointType") Integer pointType);
}
