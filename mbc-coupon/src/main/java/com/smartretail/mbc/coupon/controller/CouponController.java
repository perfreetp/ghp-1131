package com.smartretail.mbc.coupon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.coupon.dto.*;
import com.smartretail.mbc.coupon.service.CouponService;
import com.smartretail.mbc.coupon.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "优惠券包模块")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "创建券模板", description = "创建优惠券模板，支持满减券和兑换券")
    @PostMapping("/template/create")
    public Result<CouponTemplateVO> createTemplate(
            @Parameter(description = "创建券模板请求", required = true)
            @Valid @RequestBody CouponTemplateCreateDTO dto) {
        return Result.success(couponService.createTemplate(dto));
    }

    @Operation(summary = "查询券模板详情", description = "根据模板ID查询券模板详情")
    @GetMapping("/template/{templateId}")
    public Result<CouponTemplateVO> getTemplate(
            @Parameter(description = "券模板ID", required = true)
            @PathVariable("templateId") Long templateId) {
        return Result.success(couponService.getTemplate(templateId));
    }

    @Operation(summary = "分页查询券模板", description = "分页查询券模板列表，支持类型/状态/关键词筛选")
    @PostMapping("/template/page")
    public Result<IPage<CouponTemplateVO>> pageTemplates(
            @Parameter(description = "模板查询请求", required = true)
            @RequestBody CouponTemplateQueryDTO dto) {
        return Result.success(couponService.pageTemplates(dto));
    }

    @Operation(summary = "领取优惠券", description = "会员领取优惠券，分布式锁防并发")
    @PostMapping("/receive")
    public Result<CouponReceiveResultVO> receiveCoupon(
            @Parameter(description = "领券请求", required = true)
            @Valid @RequestBody CouponReceiveDTO dto) {
        return Result.success(couponService.receiveCoupon(dto));
    }

    @Operation(summary = "批量发券", description = "批量给指定会员发券，循环调用领券逻辑")
    @PostMapping("/batch-issue")
    public Result<List<CouponReceiveResultVO>> batchIssue(
            @Parameter(description = "批量发券请求", required = true)
            @Valid @RequestBody CouponBatchIssueDTO dto) {
        return Result.success(couponService.batchIssue(dto));
    }

    @Operation(summary = "判断券可用性", description = "判断券是否可用，校验状态/时间/满减/排除商品")
    @PostMapping("/check-availability")
    public Result<CouponAvailabilityVO> checkAvailability(
            @Parameter(description = "可用性判断请求", required = true)
            @Valid @RequestBody CouponAvailabilityDTO dto) {
        return Result.success(couponService.checkAvailability(dto));
    }

    @Operation(summary = "查询券实例详情", description = "根据实例ID查询券实例详情")
    @GetMapping("/instance/{instanceId}")
    public Result<CouponInstanceVO> getInstance(
            @Parameter(description = "券实例ID", required = true)
            @PathVariable("instanceId") Long instanceId) {
        return Result.success(couponService.getInstance(instanceId));
    }

    @Operation(summary = "分页查询用户券列表", description = "分页查询用户券列表，按状态优先级排序")
    @PostMapping("/member/page")
    public Result<IPage<CouponInstanceVO>> pageMemberCoupons(
            @Parameter(description = "用户券查询请求", required = true)
            @RequestBody CouponQueryDTO dto) {
        return Result.success(couponService.pageMemberCoupons(dto));
    }
}
