package com.smartretail.mbc.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.query.entity.GrayMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface GrayMetricMapper extends BaseMapper<GrayMetric> {

    List<GrayMetric> selectByGrayRuleIdAndDateRange(
            @Param("grayRuleId") Long grayRuleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    GrayMetric selectAggregatedByGrayRuleIdAndGroupType(
            @Param("grayRuleId") Long grayRuleId,
            @Param("groupType") Integer groupType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
