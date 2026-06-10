package com.smartretail.mbc.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.member.dto.MemberIdentityDTO;
import com.smartretail.mbc.member.dto.MemberMergeDTO;
import com.smartretail.mbc.member.dto.MemberQueryDTO;
import com.smartretail.mbc.member.dto.MemberRegisterDTO;
import com.smartretail.mbc.member.dto.MemberUpdateDTO;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import com.smartretail.mbc.member.vo.MemberVO;
import com.smartretail.mbc.member.vo.MergeResultVO;

public interface MemberService {

    MemberVO register(MemberRegisterDTO dto);

    MemberVO getById(Long memberId);

    MemberVO getByPhone(String phone);

    MemberVO getByMemberCode(String memberCode);

    MemberSimpleVO identify(MemberIdentityDTO dto);

    MemberVO update(MemberUpdateDTO dto);

    IPage<MemberVO> pageQuery(MemberQueryDTO dto);

    MergeResultVO mergeMembers(MemberMergeDTO dto);

    void evictMemberCache(Long memberId, String phone, String memberCode);
}
