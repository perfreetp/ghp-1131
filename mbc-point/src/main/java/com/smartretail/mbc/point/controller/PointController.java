package com.smartretail.mbc.point.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.point.dto.PointAddDTO;
import com.smartretail.mbc.point.dto.PointFreezeDTO;
import com.smartretail.mbc.point.dto.PointQueryDTO;
import com.smartretail.mbc.point.dto.PointRefundReturnDTO;
import com.smartretail.mbc.point.dto.PointSubtractDTO;
import com.smartretail.mbc.point.dto.PointUnfreezeDTO;
import com.smartretail.mbc.point.service.PointService;
import com.smartretail.mbc.point.vo.PointAccountVO;
import com.smartretail.mbc.point.vo.PointChangeResultVO;
import com.smartretail.mbc.point.vo.PointLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "积分账户模块")
@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "查询积分账户概览", description = "查询会员积分账户信息，包含可用/冻结/即将过期积分、累计获得/使用等")
    @GetMapping("/account/{memberId}")
    public Result<PointAccountVO> getAccountInfo(
            @Parameter(description = "会员ID", required = true)
            @PathVariable("memberId") Long memberId) {
        return Result.success(pointService.getAccountInfo(memberId));
    }

    @Operation(summary = "分页查询积分流水", description = "按条件分页查询积分变更流水记录")
    @PostMapping("/logs")
    public Result<IPage<PointLogVO>> queryLogs(
            @Parameter(description = "查询条件", required = true)
            @RequestBody PointQueryDTO dto) {
        return Result.success(pointService.queryLogs(dto));
    }

    @Operation(summary = "发放积分", description = "给会员发放积分，支持指定过期天数，加分布式锁防并发")
    @PostMapping("/add")
    public Result<PointChangeResultVO> addPoints(
            @Parameter(description = "积分发放请求", required = true)
            @Valid @RequestBody PointAddDTO dto) {
        return Result.success(pointService.addPoints(dto));
    }

    @Operation(summary = "扣减积分", description = "扣减会员可用积分，会检查可用余额是否充足")
    @PostMapping("/subtract")
    public Result<PointChangeResultVO> subtractPoints(
            @Parameter(description = "积分扣减请求", required = true)
            @Valid @RequestBody PointSubtractDTO dto) {
        return Result.success(pointService.subtractPoints(dto));
    }

    @Operation(summary = "冻结积分", description = "将可用积分转为冻结积分（如订单占用），需传入冻结订单号")
    @PostMapping("/freeze")
    public Result<PointChangeResultVO> freezePoints(
            @Parameter(description = "积分冻结请求", required = true)
            @Valid @RequestBody PointFreezeDTO dto) {
        return Result.success(pointService.freezePoints(dto));
    }

    @Operation(summary = "解冻积分", description = "将冻结积分转回可用积分（如订单取消释放）")
    @PostMapping("/unfreeze")
    public Result<PointChangeResultVO> unfreezePoints(
            @Parameter(description = "积分解冻请求", required = true)
            @Valid @RequestBody PointUnfreezeDTO dto) {
        return Result.success(pointService.unfreezePoints(dto));
    }

    @Operation(summary = "退款返还积分", description = "订单退款时返还已扣积分，幂等防重放，按原始订单号查已返还数量增量返还")
    @PostMapping("/refund-return")
    public Result<PointChangeResultVO> refundReturnPoints(
            @Parameter(description = "退款积分返还请求", required = true)
            @Valid @RequestBody PointRefundReturnDTO dto) {
        return Result.success(pointService.refundReturnPoints(dto));
    }
}
