package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.CrowdCalcDTO;
import com.smartretail.mbc.query.dto.CrowdGroupCreateDTO;
import com.smartretail.mbc.query.dto.CrowdGroupUpdateDTO;
import com.smartretail.mbc.query.dto.CrowdMemberQueryDTO;
import com.smartretail.mbc.query.vo.CrowdGroupVO;
import com.smartretail.mbc.query.vo.CrowdMemberVO;

public interface CrowdService {

    Long createCrowd(CrowdGroupCreateDTO dto);

    void updateCrowd(CrowdGroupUpdateDTO dto);

    void deleteCrowd(Long crowdId);

    CrowdGroupVO getCrowdDetail(Long crowdId);

    IPage<CrowdGroupVO> pageCrowds(String keyword, Integer crowdType, Integer status, Integer pageNum, Integer pageSize);

    Integer calcCrowd(CrowdCalcDTO dto);

    IPage<CrowdMemberVO> pageCrowdMembers(CrowdMemberQueryDTO dto);

    boolean isMemberInCrowd(Long crowdId, Long memberId);
}
