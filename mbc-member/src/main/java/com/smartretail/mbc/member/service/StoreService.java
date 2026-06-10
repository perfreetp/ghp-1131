package com.smartretail.mbc.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.member.dto.StoreQueryDTO;
import com.smartretail.mbc.member.vo.StoreVO;

import java.util.List;

public interface StoreService {

    StoreVO getByCode(String storeCode);

    List<StoreVO> listAll();

    IPage<StoreVO> page(StoreQueryDTO dto);

    StoreVO getById(Long id);

    StoreVO create(StoreVO vo);

    StoreVO update(StoreVO vo);

    void delete(Long id);
}
