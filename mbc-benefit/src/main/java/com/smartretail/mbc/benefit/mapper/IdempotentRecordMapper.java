package com.smartretail.mbc.benefit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.benefit.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecord> {

    IdempotentRecord selectByBusinessNo(@Param("businessNo") String businessNo,
                                        @Param("businessType") Integer businessType);
}
