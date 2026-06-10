package com.smartretail.mbc.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.query.entity.ReconcileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReconcileRecordMapper extends BaseMapper<ReconcileRecord> {

    List<Map<String, Object>> summaryByStore(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("storeCode") String storeCode,
                                              @Param("posCode") String posCode,
                                              @Param("reconcileStatus") Integer reconcileStatus);

    List<Map<String, Object>> summaryByPos(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("storeCode") String storeCode,
                                            @Param("posCode") String posCode,
                                            @Param("reconcileStatus") Integer reconcileStatus);

    List<Map<String, Object>> summaryByTemplate(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("storeCode") String storeCode,
                                                 @Param("posCode") String posCode,
                                                 @Param("reconcileStatus") Integer reconcileStatus);

    List<Map<String, Object>> summaryByDate(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("storeCode") String storeCode,
                                             @Param("posCode") String posCode,
                                             @Param("reconcileStatus") Integer reconcileStatus);
}
