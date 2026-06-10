package com.smartretail.mbc.query.service;

import com.smartretail.mbc.query.dto.DashboardQueryDTO;
import com.smartretail.mbc.query.vo.AbnormalStoreVO;
import com.smartretail.mbc.query.vo.DailyDashboardItemVO;
import com.smartretail.mbc.query.vo.OperationDashboardVO;

import java.util.List;

public interface DashboardService {

    OperationDashboardVO getOperationDashboard(DashboardQueryDTO dto);

    List<AbnormalStoreVO> getAbnormalStores(DashboardQueryDTO dto);

    List<DailyDashboardItemVO> getTrendData(DashboardQueryDTO dto);
}
