package com.smartretail.mbc.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.enums.BusinessTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.member.dto.StoreQueryDTO;
import com.smartretail.mbc.member.entity.StoreInfo;
import com.smartretail.mbc.member.mapper.StoreInfoMapper;
import com.smartretail.mbc.member.service.StoreService;
import com.smartretail.mbc.member.vo.StoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreInfoMapper storeInfoMapper;

    @Override
    public StoreVO getByCode(String storeCode) {
        if (!StringUtils.hasText(storeCode)) {
            return null;
        }
        StoreInfo store = storeInfoMapper.selectOne(
                new LambdaQueryWrapper<StoreInfo>()
                        .eq(StoreInfo::getStoreCode, storeCode)
                        .last("LIMIT 1")
        );
        return store == null ? null : convertToVO(store);
    }

    @Override
    public List<StoreVO> listAll() {
        List<StoreInfo> list = storeInfoMapper.selectList(
                new LambdaQueryWrapper<StoreInfo>()
                        .eq(StoreInfo::getStatus, 1)
                        .orderByAsc(StoreInfo::getStoreCode)
        );
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<StoreVO> page(StoreQueryDTO dto) {
        Page<StoreInfo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<StoreInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getStoreName())) {
            wrapper.like(StoreInfo::getStoreName, dto.getStoreName());
        }
        if (dto.getStoreType() != null) {
            wrapper.eq(StoreInfo::getStoreType, dto.getStoreType());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(StoreInfo::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(StoreInfo::getCreateTime);
        IPage<StoreInfo> storePage = storeInfoMapper.selectPage(page, wrapper);
        Page<StoreVO> voPage = new Page<>(storePage.getCurrent(), storePage.getSize(), storePage.getTotal());
        List<StoreVO> voList = storePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StoreVO getById(Long id) {
        StoreInfo store = storeInfoMapper.selectById(id);
        return store == null ? null : convertToVO(store);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreVO create(StoreVO vo) {
        StoreInfo exist = storeInfoMapper.selectOne(
                new LambdaQueryWrapper<StoreInfo>()
                        .eq(StoreInfo::getStoreCode, vo.getStoreCode())
                        .last("LIMIT 1")
        );
        if (exist != null) {
            throw new BusinessException("门店编码已存在");
        }
        StoreInfo store = new StoreInfo();
        BeanUtils.copyProperties(vo, store);
        storeInfoMapper.insert(store);
        return convertToVO(store);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreVO update(StoreVO vo) {
        StoreInfo store = storeInfoMapper.selectById(vo.getId());
        if (store == null) {
            throw new BusinessException("门店不存在");
        }
        if (StringUtils.hasText(vo.getStoreCode()) && !vo.getStoreCode().equals(store.getStoreCode())) {
            StoreInfo exist = storeInfoMapper.selectOne(
                    new LambdaQueryWrapper<StoreInfo>()
                            .eq(StoreInfo::getStoreCode, vo.getStoreCode())
                            .last("LIMIT 1")
            );
            if (exist != null && !exist.getId().equals(vo.getId())) {
                throw new BusinessException("门店编码已存在");
            }
        }
        BeanUtils.copyProperties(vo, store);
        storeInfoMapper.updateById(store);
        return convertToVO(store);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        storeInfoMapper.deleteById(id);
    }

    private StoreVO convertToVO(StoreInfo store) {
        StoreVO vo = new StoreVO();
        BeanUtils.copyProperties(store, vo);
        BusinessTypeEnum typeEnum = BusinessTypeEnum.getByCode(store.getStoreType());
        if (typeEnum != null) {
            vo.setStoreTypeName(typeEnum.getName());
        }
        return vo;
    }
}
