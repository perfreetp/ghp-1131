package com.smartretail.mbc.benefit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.dto.BenefitQueryDTO;
import com.smartretail.mbc.benefit.dto.BenefitReturnDTO;
import com.smartretail.mbc.benefit.vo.BenefitConfirmResultVO;
import com.smartretail.mbc.benefit.vo.BenefitLockResultVO;
import com.smartretail.mbc.benefit.vo.BenefitUseVO;

import java.util.List;

public interface BenefitService {

    BenefitLockResultVO lockBenefits(BenefitLockDTO dto);

    BenefitConfirmResultVO confirmBenefits(BenefitConfirmDTO dto);

    List<BenefitUseVO> returnBenefits(BenefitReturnDTO dto);

    IPage<BenefitUseVO> queryLogs(BenefitQueryDTO dto);

    void releaseExpiredLocksTask();

    void recordIdempotentStart(String businessNo, Integer businessType, String requestId, String requestParam);

    void recordIdempotentSuccess(String businessNo, Integer businessType, String requestId);

    void recordIdempotentFail(String businessNo, Integer businessType, String requestId, String errorMsg);
}
