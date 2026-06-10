package com.smartretail.mbc.query.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.query.dto.CrowdCalcDTO;
import com.smartretail.mbc.query.dto.CrowdGroupCreateDTO;
import com.smartretail.mbc.query.dto.CrowdGroupUpdateDTO;
import com.smartretail.mbc.query.dto.CrowdMemberQueryDTO;
import com.smartretail.mbc.query.service.CrowdService;
import com.smartretail.mbc.query.vo.CrowdGroupVO;
import com.smartretail.mbc.query.vo.CrowdMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "人群圈选模块")
@RestController
@RequestMapping("/query/crowd")
@RequiredArgsConstructor
public class CrowdController {

    private final CrowdService crowdService;

    @Operation(summary = "创建人群", description = "创建新的人群组，支持静态和动态人群")
    @PostMapping("/create")
    public Result<Long> createCrowd(
            @Parameter(description = "创建人群请求", required = true)
            @Valid @RequestBody CrowdGroupCreateDTO dto) {
        return Result.success(crowdService.createCrowd(dto));
    }

    @Operation(summary = "修改人群", description = "修改人群组的名称、规则和描述等信息")
    @PutMapping("/update")
    public Result<Void> updateCrowd(
            @Parameter(description = "修改人群请求", required = true)
            @Valid @RequestBody CrowdGroupUpdateDTO dto) {
        crowdService.updateCrowd(dto);
        return Result.success();
    }

    @Operation(summary = "删除人群", description = "删除人群组及其关联的成员数据")
    @DeleteMapping("/{crowdId}")
    public Result<Void> deleteCrowd(
            @Parameter(description = "人群ID", required = true)
            @PathVariable("crowdId") Long crowdId) {
        crowdService.deleteCrowd(crowdId);
        return Result.success();
    }

    @Operation(summary = "人群详情", description = "获取人群组的详细信息，包含规则列表")
    @GetMapping("/{crowdId}")
    public Result<CrowdGroupVO> getCrowdDetail(
            @Parameter(description = "人群ID", required = true)
            @PathVariable("crowdId") Long crowdId) {
        return Result.success(crowdService.getCrowdDetail(crowdId));
    }

    @Operation(summary = "分页查询人群列表", description = "分页查询人群组列表，支持关键词、类型、状态筛选")
    @PostMapping("/page")
    public Result<IPage<CrowdGroupVO>> pageCrowds(
            @Parameter(description = "关键词")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "人群类型：1静态 2动态")
            @RequestParam(value = "crowdType", required = false) Integer crowdType,
            @Parameter(description = "状态：0草稿 1已生效 2已失效")
            @RequestParam(value = "status", required = false) Integer status,
            @Parameter(description = "页码")
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量")
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(crowdService.pageCrowds(keyword, crowdType, status, pageNum, pageSize));
    }

    @Operation(summary = "计算人群", description = "根据圈选规则计算人群成员，重新生成人群成员列表")
    @PostMapping("/calc")
    public Result<Integer> calcCrowd(
            @Parameter(description = "计算人群请求", required = true)
            @Valid @RequestBody CrowdCalcDTO dto) {
        return Result.success(crowdService.calcCrowd(dto));
    }

    @Operation(summary = "群成员列表", description = "分页查询人群中的成员列表，支持关键词搜索")
    @PostMapping("/members")
    public Result<IPage<CrowdMemberVO>> pageCrowdMembers(
            @Parameter(description = "查询群成员请求", required = true)
            @Valid @RequestBody CrowdMemberQueryDTO dto) {
        return Result.success(crowdService.pageCrowdMembers(dto));
    }

    @Operation(summary = "判断会员是否在群", description = "检查指定会员是否在指定人群中")
    @GetMapping("/check-member")
    public Result<Boolean> checkMemberInCrowd(
            @Parameter(description = "人群ID", required = true)
            @RequestParam("crowdId") Long crowdId,
            @Parameter(description = "会员ID", required = true)
            @RequestParam("memberId") Long memberId) {
        return Result.success(crowdService.isMemberInCrowd(crowdId, memberId));
    }
}
