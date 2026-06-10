package com.smartretail.mbc.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponInstanceMapper extends BaseMapper<CouponInstance> {

    int countMemberDailyReceive(@Param("memberId") Long memberId, @Param("templateId") Long templateId, @Param("date") String dateStr);

    int countMemberTotalReceive(@Param("memberId") Long memberId, @Param("templateId") Long templateId);

    List<CouponInstance> selectExpiringInDays(@Param("days") Integer days, @Param("statusList") List<Integer> statusList);
}
