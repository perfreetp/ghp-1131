package com.smartretail.mbc.query.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.query.dto.ActivityCreateDTO;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.ActivityStatusDTO;
import com.smartretail.mbc.query.dto.ActivityUpdateDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.dto.MemberTimelineQueryDTO;
import com.smartretail.mbc.query.dto.MiniBenefitQueryDTO;
import com.smartretail.mbc.query.service.MemberTimelineService;
import com.smartretail.mbc.query.service.QueryService;
import com.smartretail.mbc.query.vo.MemberTimelineVO;
import com.smartretail.mbc.query.vo.ActivityEffectDetailVO;
import com.smartretail.mbc.query.vo.ActivityStatsVO;
import com.smartretail.mbc.query.vo.ConsumeRecordVO;
import com.smartretail.mbc.query.vo.DashboardStatsVO;
import com.smartretail.mbc.query.vo.MiniPersonalBenefitVO;
import com.smartretail.mbc.query.vo.PersonalBenefitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运营查询模块")
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    private final MemberTimelineService memberTimelineService;

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

    @Operation(summary = "创建活动", description = "创建运营活动，校验活动编码唯一、时间有效性、活动类型对应必填字段")
    @PostMapping("/activity/create")
    public Result<Long> createActivity(
            @Parameter(description = "创建活动请求", required = true)
            @Valid @RequestBody ActivityCreateDTO dto) {
        return Result.success(queryService.createActivity(dto));
    }

    @Operation(summary = "修改活动", description = "修改活动信息，进行中活动只能修改预算、描述等非核心字段")
    @PutMapping("/activity/update")
    public Result<Void> updateActivity(
            @Parameter(description = "修改活动请求", required = true)
            @Valid @RequestBody ActivityUpdateDTO dto) {
        queryService.updateActivity(dto);
        return Result.success();
    }

    @Operation(summary = "变更活动状态", description = "草稿→进行中(校验配置完整)；进行中→结束；任意→取消")
    @PutMapping("/activity/status")
    public Result<Void> changeStatus(
            @Parameter(description = "活动状态变更请求", required = true)
            @Valid @RequestBody ActivityStatusDTO dto) {
        queryService.changeStatus(dto);
        return Result.success();
    }

    @Operation(summary = "活动效果详情(增强版)", description = "查询单个活动的详细效果，包含券效果、等级效果、每日趋势、退款影响分析")
    @GetMapping("/activity/{activityId}/effect-detail")
    public Result<ActivityEffectDetailVO> getActivityEffectDetail(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("activityId") Long activityId) {
        return Result.success(queryService.getActivityEffectDetail(activityId));
    }

    @Operation(summary = "活动效果分页列表(增强版)", description = "分页查询活动列表，每个活动带效果摘要，比/stats更详细")
    @PostMapping("/activity/effect-page")
    public Result<IPage<ActivityEffectDetailVO>> pageActivityEffect(
            @Parameter(description = "活动效果查询请求", required = true)
            @RequestBody ActivityStatsQueryDTO dto) {
        return Result.success(queryService.pageActivityEffect(dto));
    }

    @Operation(summary = "小程序-个人中心权益总览", description = "查询小程序个人中心权益总览：会员卡、积分、生日权益、等级权益、过期提醒、优惠券分页、消费统计")
    @PostMapping("/mini/personal-benefit")
    public Result<MiniPersonalBenefitVO> getMiniPersonalBenefit(
            @Parameter(description = "小程序权益查询请求", required = true)
            @Valid @RequestBody MiniBenefitQueryDTO dto) {
        return Result.success(queryService.getMiniPersonalBenefit(dto));
    }

    @Operation(summary = "会员权益时间线", description = "按时间串起所有动作")
    @PostMapping("/member/timeline")
    public Result<MemberTimelineVO> getMemberTimeline(
            @Parameter(description = "会员时间线查询请求", required = true)
            @Valid @RequestBody MemberTimelineQueryDTO dto) {
        return Result.success(memberTimelineService.getMemberTimeline(dto));
    }
}
