package com.smartretail.mbc.query.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    Map<String, Object> selectDashboardSummary(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectStoreDashboard(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("storeCode") String storeCode,
                                                    @Param("city") String city,
                                                    @Param("province") String province);

    List<Map<String, Object>> selectActivityDashboard(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("activityId") Long activityId);

    List<Map<String, Object>> selectLevelDashboard(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("levelCode") Integer levelCode);

    List<Map<String, Object>> selectDailyDashboard(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectYesterdayStoreStats();

    List<Map<String, Object>> selectTodayStoreStats();
}
