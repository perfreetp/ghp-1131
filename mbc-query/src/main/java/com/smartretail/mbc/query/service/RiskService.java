package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.dto.RiskCheckDTO;
import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.query.entity.RiskRecord;

public interface RiskService {

    RiskCheckResultVO checkRisk(RiskCheckDTO dto);

    IPage<RiskRecord> queryRiskRecords(Integer scene, Integer riskLevel, Integer handleResult,
                                       Integer pageNum, Integer pageSize);

    void handleRiskRecord(Long recordId, Integer handleResult, String handleStaff, String handleRemark);
}
