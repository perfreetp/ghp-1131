package com.smartretail.mbc.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.order.dto.OrderCompleteDTO;
import com.smartretail.mbc.order.dto.OrderCreateDTO;
import com.smartretail.mbc.order.dto.OrderPayDTO;
import com.smartretail.mbc.order.dto.OrderQueryDTO;
import com.smartretail.mbc.order.dto.OrderRefundDTO;
import com.smartretail.mbc.order.dto.OrderValidateDTO;
import com.smartretail.mbc.order.dto.PosOrderValidateDTO;
import com.smartretail.mbc.order.vo.OrderStatisticsVO;
import com.smartretail.mbc.order.vo.OrderValidateResultVO;
import com.smartretail.mbc.order.vo.OrderVO;
import com.smartretail.mbc.order.vo.PosValidateResultVO;

public interface OrderService {

    PosValidateResultVO posValidate(PosOrderValidateDTO dto);

    OrderValidateResultVO validateOrder(OrderValidateDTO dto);

    OrderVO createOrder(OrderCreateDTO dto);

    OrderVO payOrder(OrderPayDTO dto);

    OrderVO completeOrder(OrderCompleteDTO dto);

    OrderVO refundOrder(OrderRefundDTO dto);

    IPage<OrderVO> pageOrders(OrderQueryDTO dto);

    OrderStatisticsVO getStatistics(OrderQueryDTO dto);
}
