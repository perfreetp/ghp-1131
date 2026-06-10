package com.smartretail.mbc.level.controller;

import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.level.dto.BirthdayBenefitDTO;
import com.smartretail.mbc.level.dto.GrowthCalcDTO;
import com.smartretail.mbc.level.dto.LevelAdjustDTO;
import com.smartretail.mbc.level.dto.LevelRuleUpsertDTO;
import com.smartretail.mbc.level.service.LevelService;
import com.smartretail.mbc.level.vo.BirthdayBenefitResultVO;
import com.smartretail.mbc.level.vo.GrowthResultVO;
import com.smartretail.mbc.level.vo.LevelRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "等级规则模块")
@RestController
@RequestMapping("/level")
@RequiredArgsConstructor
public class LevelController {

    private final LevelService levelService;

    @Operation(summary = "查询所有等级规则", description = "查询所有等级规则（启用+禁用），包含各等级会员数")
    @GetMapping("/rules")
    public Result<List<LevelRuleVO>> listAllRules() {
        return Result.success(levelService.listAllRules());
    }

    @Operation(summary = "按编码查询等级规则", description = "根据等级编码查询等级规则详情")
    @GetMapping("/rule/{levelCode}")
    public Result<LevelRuleVO> getRuleByCode(
            @Parameter(description = "等级编码", required = true)
            @PathVariable("levelCode") Integer levelCode) {
        return Result.success(levelService.getRuleByCode(levelCode));
    }

    @Operation(summary = "新增或修改等级规则", description = "新增或修改等级规则配置")
    @PostMapping("/rule/upsert")
    public Result<Void> upsertRule(
            @Parameter(description = "等级规则信息", required = true)
            @RequestBody LevelRuleUpsertDTO dto) {
        levelService.upsertRule(dto);
        return Result.success();
    }

    @Operation(summary = "查询会员当前等级", description = "查询会员当前等级详情（含等级名称、权益等）")
    @GetMapping("/current/{memberId}")
    public Result<LevelRuleVO> getCurrentLevel(
            @Parameter(description = "会员ID", required = true)
            @PathVariable("memberId") Long memberId) {
        return Result.success(levelService.getCurrentLevel(memberId));
    }

    @Operation(summary = "计算并增加成长值", description = "根据订单金额和等级倍率计算成长值并累加，判定是否升级")
    @PostMapping("/growth/calc")
    public Result<GrowthResultVO> calcAndAddGrowth(
            @Parameter(description = "成长值计算请求", required = true)
            @Valid @RequestBody GrowthCalcDTO dto) {
        return Result.success(levelService.calcAndAddGrowth(dto));
    }

    @Operation(summary = "人工调整成长值", description = "人工调整成长值（支持负数），重新判定等级并记录日志")
    @PostMapping("/growth/adjust")
    public Result<GrowthResultVO> adjustLevel(
            @Parameter(description = "等级调整请求", required = true)
            @Valid @RequestBody LevelAdjustDTO dto) {
        return Result.success(levelService.adjustLevel(dto));
    }

    @Operation(summary = "发放生日权益", description = "按会员等级发放生日积分和优惠券，Redis防重复领取")
    @PostMapping("/birthday/grant")
    public Result<BirthdayBenefitResultVO> grantBirthdayBenefit(
            @Parameter(description = "生日权益发放请求", required = true)
            @Valid @RequestBody BirthdayBenefitDTO dto) {
        return Result.success(levelService.grantBirthdayBenefit(dto));
    }
}
