package com.smartretail.mbc.query.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.query.dto.ReconcileDetailQueryDTO;
import com.smartretail.mbc.query.dto.ReconcileQueryDTO;
import com.smartretail.mbc.query.vo.ReconcileDetailVO;
import com.smartretail.mbc.query.vo.ReconcileResultVO;

import java.time.LocalDate;

public interface ReconcileService {

    ReconcileResultVO getReconcileSummary(ReconcileQueryDTO dto);

    IPage<ReconcileDetailVO> getReconcileDetail(ReconcileDetailQueryDTO dto);

    void executeReconcile(LocalDate date);
}
