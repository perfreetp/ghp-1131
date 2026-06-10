package com.smartretail.mbc.query.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.service.QueryService;
import com.smartretail.mbc.query.vo.ActivityStatsVO;
import com.smartretail.mbc.query.vo.ConsumeRecordVO;
import com.smartretail.mbc.query.vo.DashboardStatsVO;
import com.smartretail.mbc.query.vo.PersonalBenefitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运营查询模块")
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @Operation(summary = "查询消费记录", description = "按会员ID分页查询消费记录，支持时间范围、订单类型、金额、门店筛选")
    @PostMapping("/consume/records")
    public Result<IPage<ConsumeRecordVO>> queryConsumeRecords(
            @Parameter(description = "消费记录查询请求", required = true)
            @Valid @RequestBody ConsumeRecordQueryDTO dto) {
        return Result.success(queryService.queryConsumeRecords(dto));
    }

    @Operation(summary = "个人权益清单总览", description = "查询会员的积分、券、等级权益、消费记录等综合权益信息")
    @PostMapping("/personal/benefits")
    public Result<PersonalBenefitVO> getPersonalBenefitList(
            @Parameter(description = "个人权益清单查询请求", required = true)
            @Valid @RequestBody BenefitListQueryDTO dto) {
        return Result.success(queryService.getPersonalBenefitList(dto));
    }

    @Operation(summary = "活动效果统计列表", description = "分页查询活动效果统计，含预算使用率、漏斗转化率、ROI等指标")
    @PostMapping("/activity/stats")
    public Result<IPage<ActivityStatsVO>> queryActivityStats(
            @Parameter(description = "活动效果统计查询请求", required = true)
            @RequestBody ActivityStatsQueryDTO dto) {
        return Result.success(queryService.queryActivityStats(dto));
    }

    @Operation(summary = "单个活动详细统计", description = "查询单个活动的详细统计数据，包含每日趋势和参与者等级分布")
    @GetMapping("/activity/{activityId}/stats")
    public Result<ActivityStatsVO> getActivityDetailStats(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("activityId") Long activityId) {
        return Result.success(queryService.getActivityDetailStats(activityId));
    }

    @Operation(summary = "运营大盘", description = "获取运营大盘数据：会员统计、订单统计、权益统计、Top活动、等级分布")
    @PostMapping("/dashboard")
    public Result<DashboardStatsVO> getDashboardStats(
            @Parameter(description = "运营大盘查询请求", required = true)
            @RequestBody DashboardStatsDTO dto) {
        return Result.success(queryService.getDashboardStats(dto));
    }
}
