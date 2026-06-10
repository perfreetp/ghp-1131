package com.smartretail.mbc.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.query.entity.CrowdMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CrowdMemberMapper extends BaseMapper<CrowdMember> {

    void batchInsert(@Param("list") List<CrowdMember> list);

    Integer countByCrowdId(@Param("crowdId") Long crowdId);

    List<Long> selectMemberIdsByCrowdId(@Param("crowdId") Long crowdId, @Param("limit") Integer limit);
}
