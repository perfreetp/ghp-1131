package com.smartretail.mbc.order.service;

import com.smartretail.mbc.order.dto.BenefitFulfillmentQueryDTO;
import com.smartretail.mbc.order.vo.BenefitFulfillmentVO;

import java.util.List;

public interface BenefitFulfillmentService {

    BenefitFulfillmentVO getFulfillmentStatus(BenefitFulfillmentQueryDTO dto);

    List<BenefitFulfillmentVO> getMemberFulfillmentList(Long memberId, Integer pageNum, Integer pageSize);
}
