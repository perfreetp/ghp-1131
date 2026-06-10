package com.smartretail.mbc.point.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.point.dto.PointAddDTO;
import com.smartretail.mbc.point.dto.PointFreezeDTO;
import com.smartretail.mbc.point.dto.PointQueryDTO;
import com.smartretail.mbc.point.dto.PointRefundReturnDTO;
import com.smartretail.mbc.point.dto.PointSubtractDTO;
import com.smartretail.mbc.point.dto.PointUnfreezeDTO;
import com.smartretail.mbc.point.vo.PointAccountVO;
import com.smartretail.mbc.point.vo.PointChangeResultVO;
import com.smartretail.mbc.point.vo.PointLogVO;

public interface PointService {

    PointAccountVO getAccountInfo(Long memberId);

    IPage<PointLogVO> queryLogs(PointQueryDTO dto);

    PointChangeResultVO addPoints(PointAddDTO dto);

    PointChangeResultVO subtractPoints(PointSubtractDTO dto);

    PointChangeResultVO freezePoints(PointFreezeDTO dto);

    PointChangeResultVO unfreezePoints(PointUnfreezeDTO dto);

    PointChangeResultVO refundReturnPoints(PointRefundReturnDTO dto);

    void expirePointsTask();

    int countExpiringInDays(Long memberId, int days);
}
