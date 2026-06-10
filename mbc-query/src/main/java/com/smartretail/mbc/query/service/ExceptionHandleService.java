package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.BenefitChainQueryDTO;
import com.smartretail.mbc.query.dto.IdempotentHandleDTO;
import com.smartretail.mbc.query.vo.BenefitChainVO;
import com.smartretail.mbc.query.vo.IdempotentRecordVO;

public interface ExceptionHandleService {

    BenefitChainVO getBenefitChain(BenefitChainQueryDTO dto);

    IdempotentRecordVO handleIdempotent(IdempotentHandleDTO dto);

    IPage<IdempotentRecordVO> queryIdempotentRecords(Integer processStatus, Integer pageNum, Integer pageSize);
}
