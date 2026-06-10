package com.smartretail.mbc.benefit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BenefitUseLogMapper extends BaseMapper<BenefitUseLog> {

    List<BenefitUseLog> selectByOrderNo(@Param("orderNo") String orderNo);

    List<BenefitUseLog> selectLockedWithinMinutes(@Param("minutes") Integer minutes);
}
