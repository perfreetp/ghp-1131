package com.smartretail.mbc.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.member.dto.MemberIdentityDTO;
import com.smartretail.mbc.member.dto.MemberMergeDTO;
import com.smartretail.mbc.member.dto.MemberQueryDTO;
import com.smartretail.mbc.member.dto.MemberRegisterDTO;
import com.smartretail.mbc.member.dto.MemberUpdateDTO;
import com.smartretail.mbc.member.service.MemberService;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import com.smartretail.mbc.member.vo.MemberVO;
import com.smartretail.mbc.member.vo.MergeResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "会员识别模块")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "会员注册", description = "通过手机号注册新会员，自动生成会员码，初始化等级和积分")
    @PostMapping("/register")
    public Result<MemberVO> register(
            @Parameter(description = "注册信息", required = true)
            @Valid @RequestBody MemberRegisterDTO dto) {
        return Result.success(memberService.register(dto));
    }

    @Operation(summary = "根据ID查询会员", description = "根据会员ID查询会员详情信息，含等级名称和权益")
    @GetMapping("/{id}")
    public Result<MemberVO> getById(
            @Parameter(description = "会员ID", required = true)
            @PathVariable("id") Long id) {
        return Result.success(memberService.getById(id));
    }

    @Operation(summary = "根据手机号查询会员", description = "通过手机号查询会员信息，优先走缓存")
    @GetMapping("/by-phone/{phone}")
    public Result<MemberVO> getByPhone(
            @Parameter(description = "手机号", required = true)
            @PathVariable("phone") String phone) {
        return Result.success(memberService.getByPhone(phone));
    }

    @Operation(summary = "根据会员码查询会员", description = "通过会员码查询会员信息，优先走缓存")
    @GetMapping("/by-code/{memberCode}")
    public Result<MemberVO> getByMemberCode(
            @Parameter(description = "会员码", required = true)
            @PathVariable("memberCode") String memberCode) {
        return Result.success(memberService.getByMemberCode(memberCode));
    }

    @Operation(summary = "会员身份识别", description = "通过手机号或会员码识别会员身份，返回简单信息；至少提供手机号或会员码之一")
    @PostMapping("/identify")
    public Result<MemberSimpleVO> identify(
            @Parameter(description = "身份识别请求", required = true)
            @RequestBody MemberIdentityDTO dto) {
        return Result.success(memberService.identify(dto));
    }

    @Operation(summary = "更新会员信息", description = "更新会员的基本信息，更新后清除缓存")
    @PutMapping("/update")
    public Result<MemberVO> update(
            @Parameter(description = "更新信息", required = true)
            @Valid @RequestBody MemberUpdateDTO dto) {
        return Result.success(memberService.update(dto));
    }

    @Operation(summary = "分页查询会员列表", description = "按条件分页查询会员列表，支持手机号、会员码、姓名、等级、状态过滤")
    @PostMapping("/page")
    public Result<IPage<MemberVO>> pageQuery(
            @Parameter(description = "查询条件", required = true)
            @RequestBody MemberQueryDTO dto) {
        return Result.success(memberService.pageQuery(dto));
    }

    @Operation(summary = "合并会员", description = "将两个会员账户合并，迁移积分、成长值和优惠券，被合并的会员标记为已合并状态")
    @PostMapping("/merge")
    public Result<MergeResultVO> mergeMembers(
            @Parameter(description = "合并请求", required = true)
            @Valid @RequestBody MemberMergeDTO dto) {
        return Result.success(memberService.mergeMembers(dto));
    }
}
