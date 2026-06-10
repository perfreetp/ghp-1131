package com.smartretail.mbc.query.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.dto.RiskCheckDTO;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.query.dto.ActivityBudgetCreateDTO;
import com.smartretail.mbc.query.dto.ActivityBudgetQueryDTO;
import com.smartretail.mbc.query.dto.ActivityCreateDTO;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.ActivityStatusDTO;
import com.smartretail.mbc.query.dto.ActivityUpdateDTO;
import com.smartretail.mbc.query.dto.BenefitChainQueryDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.dto.IdempotentHandleDTO;
import com.smartretail.mbc.query.dto.MemberTimelineQueryDTO;
import com.smartretail.mbc.query.dto.MiniBenefitQueryDTO;
import com.smartretail.mbc.query.dto.ReconcileDetailQueryDTO;
import com.smartretail.mbc.query.dto.ReconcileQueryDTO;
import com.smartretail.mbc.query.entity.ActivityBudgetLog;
import com.smartretail.mbc.query.entity.RiskRecord;
import com.smartretail.mbc.query.service.ActivityBudgetService;
import com.smartretail.mbc.query.service.ExceptionHandleService;
import com.smartretail.mbc.query.service.MemberTimelineService;
import com.smartretail.mbc.query.service.QueryService;
import com.smartretail.mbc.query.service.RiskService;
import com.smartretail.mbc.query.service.ReconcileService;
import com.smartretail.mbc.query.vo.MemberTimelineVO;
import com.smartretail.mbc.query.vo.BenefitChainVO;
import com.smartretail.mbc.query.vo.IdempotentRecordVO;
import com.smartretail.mbc.query.vo.ReconcileDetailVO;
import com.smartretail.mbc.query.vo.ReconcileResultVO;
import com.smartretail.mbc.query.vo.ActivityBudgetProgressVO;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "运营查询模块")
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    private final ActivityBudgetService activityBudgetService;

    private final MemberTimelineService memberTimelineService;

    private final ReconcileService reconcileService;

    private final RiskService riskService;

    private final ExceptionHandleService exceptionHandleService;

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

    @Operation(summary = "对账汇总", description = "按门店/POS/模板/日期维度汇总对账数据")
    @PostMapping("/reconcile/summary")
    public Result<ReconcileResultVO> getReconcileSummary(
            @Parameter(description = "对账查询请求", required = true)
            @Valid @RequestBody ReconcileQueryDTO dto) {
        return Result.success(reconcileService.getReconcileSummary(dto));
    }

    @Operation(summary = "对账明细钻取", description = "分页查询对账明细记录")
    @PostMapping("/reconcile/detail")
    public Result<IPage<ReconcileDetailVO>> getReconcileDetail(
            @Parameter(description = "对账明细查询请求", required = true)
            @Valid @RequestBody ReconcileDetailQueryDTO dto) {
        return Result.success(reconcileService.getReconcileDetail(dto));
    }

    @Operation(summary = "手动执行对账", description = "执行指定日期的权益核销对账")
    @PostMapping("/reconcile/execute")
    public Result<Void> executeReconcile(
            @Parameter(description = "对账日期", required = true)
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        reconcileService.executeReconcile(date);
        return Result.success();
    }

    @Operation(summary = "风控检查", description = "对领券/试算/退款/核销等场景进行风控检查")
    @PostMapping("/risk/check")
    public Result<RiskCheckResultVO> checkRisk(
            @Parameter(description = "风控检查请求", required = true)
            @Valid @RequestBody RiskCheckDTO dto) {
        return Result.success(riskService.checkRisk(dto));
    }

    @Operation(summary = "查询风控记录", description = "按场景/风险等级/处置结果分页查询风控记录")
    @PostMapping("/risk/records")
    public Result<IPage<RiskRecord>> queryRiskRecords(
            @Parameter(description = "风控场景") @RequestParam(required = false) Integer scene,
            @Parameter(description = "风险等级") @RequestParam(required = false) Integer riskLevel,
            @Parameter(description = "处置结果") @RequestParam(required = false) Integer handleResult,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(riskService.queryRiskRecords(scene, riskLevel, handleResult, pageNum, pageSize));
    }

    @Operation(summary = "人工处置风控记录", description = "对风控记录进行放行/确认/拦截处置")
    @PostMapping("/risk/handle")
    public Result<Void> handleRiskRecord(
            @Parameter(description = "记录ID", required = true) @RequestParam Long recordId,
            @Parameter(description = "处置结果: 1放行 2人工确认 3拦截", required = true) @RequestParam Integer handleResult,
            @Parameter(description = "处置人") @RequestParam(required = false) String handleStaff,
            @Parameter(description = "处置备注") @RequestParam(required = false) String handleRemark) {
        riskService.handleRiskRecord(recordId, handleResult, handleStaff, handleRemark);
        return Result.success();
    }

    @Operation(summary = "创建活动预算", description = "为活动创建总预算或门店级预算配置")
    @PostMapping("/activity/budget/create")
    public Result<Void> createActivityBudget(
            @Parameter(description = "创建活动预算请求", required = true)
            @Valid @RequestBody ActivityBudgetCreateDTO dto) {
        activityBudgetService.createActivityBudget(dto);
        return Result.success();
    }

    @Operation(summary = "查看预算进度", description = "查询活动预算消耗进度，含各门店预算明细")
    @GetMapping("/activity/budget/progress/{activityId}")
    public Result<ActivityBudgetProgressVO> getBudgetProgress(
            @Parameter(description = "活动ID", required = true)
            @PathVariable("activityId") Long activityId) {
        return Result.success(activityBudgetService.getBudgetProgress(activityId));
    }

    @Operation(summary = "预算变动日志", description = "分页查询活动预算变动日志")
    @PostMapping("/activity/budget/logs")
    public Result<IPage<ActivityBudgetLog>> getBudgetLogs(
            @Parameter(description = "预算日志查询请求", required = true)
            @Valid @RequestBody ActivityBudgetQueryDTO dto) {
        return Result.success(activityBudgetService.getBudgetLogs(dto));
    }

    @Operation(summary = "查询权益处理链路", description = "按订单号或退款单号查询整条权益处理链路，包含锁定、核销、返还步骤及关联幂等记录")
    @PostMapping("/benefit/chain")
    public Result<BenefitChainVO> getBenefitChain(
            @Parameter(description = "权益链路查询请求", required = true)
            @Valid @RequestBody BenefitChainQueryDTO dto) {
        return Result.success(exceptionHandleService.getBenefitChain(dto));
    }

    @Operation(summary = "人工处理幂等记录", description = "对处理中或失败的幂等请求支持人工重放或标记失败")
    @PostMapping("/idempotent/handle")
    public Result<IdempotentRecordVO> handleIdempotent(
            @Parameter(description = "人工处理幂等请求", required = true)
            @Valid @RequestBody IdempotentHandleDTO dto) {
        return Result.success(exceptionHandleService.handleIdempotent(dto));
    }

    @Operation(summary = "查询待处理幂等记录", description = "分页查询幂等处理记录，支持按处理状态筛选")
    @PostMapping("/idempotent/records")
    public Result<IPage<IdempotentRecordVO>> queryIdempotentRecords(
            @Parameter(description = "处理状态: 1处理中 2已完成 3已失败") @RequestParam(required = false) Integer processStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(exceptionHandleService.queryIdempotentRecords(processStatus, pageNum, pageSize));
    }
}
