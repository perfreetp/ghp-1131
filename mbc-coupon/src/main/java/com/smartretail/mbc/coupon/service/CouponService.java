package com.smartretail.mbc.coupon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.coupon.dto.*;
import com.smartretail.mbc.coupon.vo.*;

import java.util.List;

public interface CouponService {

    CouponTemplateVO createTemplate(CouponTemplateCreateDTO dto);

    CouponTemplateVO getTemplate(Long templateId);

    IPage<CouponTemplateVO> pageTemplates(CouponTemplateQueryDTO dto);

    CouponReceiveResultVO receiveCoupon(CouponReceiveDTO dto);

    List<CouponReceiveResultVO> batchIssue(CouponBatchIssueDTO dto);

    CouponAvailabilityVO checkAvailability(CouponAvailabilityDTO dto);

    IPage<CouponInstanceVO> pageMemberCoupons(CouponQueryDTO dto);

    CouponInstanceVO getInstance(Long instanceId);

    void updateInstanceStatus(Long instanceId, Integer targetStatus, Integer expectedStatus);

    void expireCouponsTask();

    void pushExpireReminderTask();
}
