package com.smartretail.mbc.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.message.entity.MessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLog> {

    int countUnreadByMemberId(@Param("memberId") Long memberId);

    List<Map<String, Object>> countUnreadGroupByType(@Param("memberId") Long memberId);
}
