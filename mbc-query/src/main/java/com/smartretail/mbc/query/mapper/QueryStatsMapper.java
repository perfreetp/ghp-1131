package com.smartretail.mbc.query.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface QueryStatsMapper {

    Map<String, Object> sumMemberConsume(@Param("memberId") Long memberId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    Long countMemberConsume(@Param("memberId") Long memberId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    List<Map<String, Object>> countMembersByLevel();

    Long countNewMembersBetween(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    Long countActiveMembersBetween(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    Map<String, Object> sumOrdersBetween(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("storeCode") String storeCode);

    Long countMemberOrdersBetween(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    Map<String, Object> sumCouponsBetween(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    List<Map<String, Object>> selectActivityDailyTrend(@Param("activityId") Long activityId);

    List<Map<String, Object>> countActivityParticipantsByLevel(@Param("activityId") Long activityId);

    Map<String, Object> selectActivityCouponEffect(@Param("activityId") Long activityId);

    List<Map<String, Object>> selectActivityMemberLevels(@Param("activityId") Long activityId);

    List<Map<String, Object>> selectActivityDailyDetail(@Param("activityId") Long activityId);

    Map<String, Object> selectActivityRefundImpact(@Param("activityId") Long activityId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    Long countActivityNewMembers(@Param("activityId") Long activityId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}
