package com.smartretail.mbc.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumeOrderMapper extends BaseMapper<ConsumeOrder> {

    ConsumeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("oldStatus") Integer oldStatus,
                          @Param("newStatus") Integer newStatus);
}
