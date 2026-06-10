package com.smartretail.mbc.benefit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.dto.BenefitQueryDTO;
import com.smartretail.mbc.benefit.dto.BenefitReturnDTO;
import com.smartretail.mbc.benefit.service.BenefitService;
import com.smartretail.mbc.benefit.vo.BenefitConfirmResultVO;
import com.smartretail.mbc.benefit.vo.BenefitLockResultVO;
import com.smartretail.mbc.benefit.vo.BenefitUseVO;
import com.smartretail.mbc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "权益核销模块")
@RestController
@RequestMapping("/benefit")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    @Operation(summary = "锁定权益", description = "下单时锁定优惠券/积分抵扣/等级折扣/兑换权益，支持批量锁券")
    @PostMapping("/lock")
    public Result<BenefitLockResultVO> lockBenefits(
            @Parameter(description = "锁定权益请求", required = true)
            @Valid @RequestBody BenefitLockDTO dto) {
        return Result.success(benefitService.lockBenefits(dto));
    }

    @Operation(summary = "确认核销", description = "支付成功后完成核销，确认券使用、积分扣减等")
    @PostMapping("/confirm")
    public Result<BenefitConfirmResultVO> confirmBenefits(
            @Parameter(description = "确认核销请求", required = true)
            @Valid @RequestBody BenefitConfirmDTO dto) {
        return Result.success(benefitService.confirmBenefits(dto));
    }

    @Operation(summary = "权益返还（退款）", description = "退款时返还已核销的权益，券恢复可用，积分返还")
    @PostMapping("/return")
    public Result<List<BenefitUseVO>> returnBenefits(
            @Parameter(description = "权益返还请求", required = true)
            @Valid @RequestBody BenefitReturnDTO dto) {
        return Result.success(benefitService.returnBenefits(dto));
    }

    @Operation(summary = "分页查询核销记录", description = "按会员/状态/类型/订单号/时间范围查询核销记录")
    @PostMapping("/logs")
    public Result<IPage<BenefitUseVO>> queryLogs(
            @Parameter(description = "核销记录查询请求", required = true)
            @RequestBody BenefitQueryDTO dto) {
        return Result.success(benefitService.queryLogs(dto));
    }
}
