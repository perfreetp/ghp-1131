package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.ActivityStatsQueryDTO;
import com.smartretail.mbc.query.dto.BenefitListQueryDTO;
import com.smartretail.mbc.query.dto.ConsumeRecordQueryDTO;
import com.smartretail.mbc.query.dto.DashboardStatsDTO;
import com.smartretail.mbc.query.vo.ActivityStatsVO;
import com.smartretail.mbc.query.vo.ConsumeRecordVO;
import com.smartretail.mbc.query.vo.DashboardStatsVO;
import com.smartretail.mbc.query.vo.PersonalBenefitVO;

public interface QueryService {

    IPage<ConsumeRecordVO> queryConsumeRecords(ConsumeRecordQueryDTO dto);

    PersonalBenefitVO getPersonalBenefitList(BenefitListQueryDTO dto);

    IPage<ActivityStatsVO> queryActivityStats(ActivityStatsQueryDTO dto);

    ActivityStatsVO getActivityDetailStats(Long activityId);

    DashboardStatsVO getDashboardStats(DashboardStatsDTO dto);
}
