package com.smartretail.mbc.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.order.dto.OrderCompleteDTO;
import com.smartretail.mbc.order.dto.OrderCreateDTO;
import com.smartretail.mbc.order.dto.OrderPayDTO;
import com.smartretail.mbc.order.dto.OrderQueryDTO;
import com.smartretail.mbc.order.dto.OrderRefundDTO;
import com.smartretail.mbc.order.dto.OrderValidateDTO;
import com.smartretail.mbc.order.dto.PosOrderValidateDTO;
import com.smartretail.mbc.order.service.OrderService;
import com.smartretail.mbc.order.vo.OrderStatisticsVO;
import com.smartretail.mbc.order.vo.OrderValidateResultVO;
import com.smartretail.mbc.order.vo.OrderVO;
import com.smartretail.mbc.order.vo.PosValidateResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单模块", description = "订单校验、创建、支付、完成、退款等接口")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "收银端订单试算", description = "收银端完整下单试算：会员识别、商品校验、排除商品识别、券试用、券组合推荐、积分计算、等级折扣、最终金额计算等")
    @PostMapping("/pos/validate")
    public Result<PosValidateResultVO> posValidate(
            @Parameter(description = "收银端试算请求", required = true)
            @Valid @RequestBody PosOrderValidateDTO dto) {
        return Result.success(orderService.posValidate(dto));
    }

    @Operation(summary = "订单预校验", description = "下单前预校验会员、优惠券、积分、等级折扣等，不写DB")
    @PostMapping("/validate")
    public Result<OrderValidateResultVO> validateOrder(
            @Parameter(description = "订单预校验请求", required = true)
            @Valid @RequestBody OrderValidateDTO dto) {
        return Result.success(orderService.validateOrder(dto));
    }

    @Operation(summary = "创建订单", description = "创建订单（幂等），先预校验，保存订单（状态为待支付），不锁权益")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(
            @Parameter(description = "创建订单请求", required = true)
            @Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @Operation(summary = "支付完成", description = "支付完成，更新订单状态为已支付，锁定所有权益")
    @PostMapping("/pay")
    public Result<OrderVO> payOrder(
            @Parameter(description = "支付完成请求", required = true)
            @Valid @RequestBody OrderPayDTO dto) {
        return Result.success(orderService.payOrder(dto));
    }

    @Operation(summary = "完成订单", description = "完成订单，确认权益、发放积分、增加成长值、更新券使用次数")
    @PostMapping("/complete")
    public Result<OrderVO> completeOrder(
            @Parameter(description = "订单完成请求", required = true)
            @Valid @RequestBody OrderCompleteDTO dto) {
        return Result.success(orderService.completeOrder(dto));
    }

    @Operation(summary = "退款订单", description = "退款订单，更新订单状态为已退款，返还已核销权益")
    @PostMapping("/refund")
    public Result<OrderVO> refundOrder(
            @Parameter(description = "退款请求", required = true)
            @Valid @RequestBody OrderRefundDTO dto) {
        return Result.success(orderService.refundOrder(dto));
    }

    @Operation(summary = "分页查询订单", description = "按条件分页查询订单列表")
    @PostMapping("/page")
    public Result<IPage<OrderVO>> pageOrders(
            @Parameter(description = "订单查询请求", required = true)
            @RequestBody OrderQueryDTO dto) {
        return Result.success(orderService.pageOrders(dto));
    }

    @Operation(summary = "订单统计", description = "统计订单数量、支付金额、优惠金额、会员订单占比等")
    @PostMapping("/statistics")
    public Result<OrderStatisticsVO> getStatistics(
            @Parameter(description = "订单查询条件", required = true)
            @RequestBody OrderQueryDTO dto) {
        return Result.success(orderService.getStatistics(dto));
    }
}
