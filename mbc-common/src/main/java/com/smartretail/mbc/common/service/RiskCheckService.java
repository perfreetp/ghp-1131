package com.smartretail.mbc.common.service;

import com.smartretail.mbc.common.dto.RiskCheckDTO;
import com.smartretail.mbc.common.vo.RiskCheckResultVO;

public interface RiskCheckService {

    RiskCheckResultVO checkRisk(RiskCheckDTO dto);
}
