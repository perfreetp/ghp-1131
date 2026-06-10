package com.smartretail.mbc.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.service.BenefitService;
import com.smartretail.mbc.benefit.vo.BenefitConfirmResultVO;
import com.smartretail.mbc.benefit.vo.BenefitLockResultVO;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.order.dto.OfflinePreLockDTO;
import com.smartretail.mbc.order.entity.OfflinePreLock;
import com.smartretail.mbc.order.mapper.OfflinePreLockMapper;
import com.smartretail.mbc.order.service.OfflineSyncService;
import com.smartretail.mbc.order.vo.OfflinePreLockVO;
import com.smartretail.mbc.order.vo.OfflineSyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineSyncServiceImpl implements OfflineSyncService {

    private final OfflinePreLockMapper offlinePreLockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final BenefitService benefitService;

    private static final int SYNC_STATUS_PENDING = 0;
    private static final int SYNC_STATUS_PROCESSING = 1;
    private static final int SYNC_STATUS_SUCCESS = 2;
    private static final int SYNC_STATUS_FAILED = 3;

    private static final int LOCK_EXPIRE_SECONDS = 300;
    private static final int MAX_RETRY_COUNT = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfflineSyncResultVO syncOfflinePreLock(OfflinePreLockDTO dto) {
        String offlineLockNo = dto.getOfflineLockNo();

        OfflinePreLock existRecord = offlinePreLockMapper.selectByOfflineLockNo(offlineLockNo);

        if (existRecord != null) {
            Integer syncStatus = existRecord.getSyncStatus();

            if (SYNC_STATUS_SUCCESS == syncStatus) {
                return buildIdempotentResult(existRecord);
            }

            if (SYNC_STATUS_PROCESSING == syncStatus) {
                OfflineSyncResultVO result = new OfflineSyncResultVO();
                result.setOfflineLockNo(offlineLockNo);
                result.setSyncStatus(SYNC_STATUS_PROCESSING);
                result.setSyncStatusName("同步中");
                result.setIsIdempotent(false);
                result.setMessage("同步中，请稍后重试");
                return result;
            }
        } else {
            existRecord = createOfflinePreLock(dto);
        }

        String lockKey = RedisKeyUtil.offlineSync(offlineLockNo);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            OfflineSyncResultVO result = new OfflineSyncResultVO();
            result.setOfflineLockNo(offlineLockNo);
            result.setSyncStatus(SYNC_STATUS_PROCESSING);
            result.setSyncStatusName("同步中");
            result.setIsIdempotent(false);
            result.setMessage("同步中，请稍后重试");
            return result;
        }

        try {
            OfflinePreLock currentRecord = offlinePreLockMapper.selectByOfflineLockNo(offlineLockNo);
            if (currentRecord == null) {
                throw new BusinessException("离线预锁记录不存在");
            }

            if (SYNC_STATUS_SUCCESS == currentRecord.getSyncStatus()) {
                return buildIdempotentResult(currentRecord);
            }

            currentRecord.setSyncStatus(SYNC_STATUS_PROCESSING);
            offlinePreLockMapper.updateById(currentRecord);

            return doSync(currentRecord, dto);

        } catch (Exception e) {
            log.error("离线同步异常, offlineLockNo: {}", offlineLockNo, e);

            OfflinePreLock failRecord = offlinePreLockMapper.selectByOfflineLockNo(offlineLockNo);
            if (failRecord != null) {
                failRecord.setSyncStatus(SYNC_STATUS_FAILED);
                failRecord.setSyncErrorMsg(e.getMessage());
                failRecord.setSyncRetryCount(
                        failRecord.getSyncRetryCount() == null ? 1 : failRecord.getSyncRetryCount() + 1);
                offlinePreLockMapper.updateById(failRecord);
            }

            throw new BusinessException("离线同步失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public IPage<OfflinePreLockVO> queryOfflineRecords(String storeCode, Integer syncStatus,
                                                       Integer pageNum, Integer pageSize) {
        Page<OfflinePreLock> page = new Page<>(
                pageNum != null ? pageNum : 1,
                pageSize != null ? pageSize : 10);

        IPage<OfflinePreLock> resultPage = offlinePreLockMapper.selectByStoreAndStatus(
                storeCode, syncStatus, page);

        return resultPage.convert(this::convertToVO);
    }

    @Override
    public void retryFailedOfflineLocks(String storeCode) {
        Page<OfflinePreLock> page = new Page<>(1, 100);
        IPage<OfflinePreLock> failedRecords = offlinePreLockMapper.selectByStoreAndStatus(
                storeCode, SYNC_STATUS_FAILED, page);

        for (OfflinePreLock record : failedRecords.getRecords()) {
            if (record.getSyncRetryCount() != null && record.getSyncRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("离线记录重试次数已达上限, offlineLockNo: {}, retryCount: {}",
                        record.getOfflineLockNo(), record.getSyncRetryCount());
                continue;
            }

            try {
                OfflinePreLockDTO dto = new OfflinePreLockDTO();
                BeanUtils.copyProperties(record, dto);

                if (record.getCouponIds() != null) {
                    try {
                        List<Long> couponIdList = objectMapper.readValue(
                                record.getCouponIds(),
                                new TypeReference<List<Long>>() {});
                        dto.setCouponIds(couponIdList);
                    } catch (Exception e) {
                        log.warn("解析券ID列表失败, offlineLockNo: {}", record.getOfflineLockNo(), e);
                    }
                }

                syncOfflinePreLock(dto);
            } catch (Exception e) {
                log.error("重试离线同步失败, offlineLockNo: {}", record.getOfflineLockNo(), e);
            }
        }
    }

    private OfflinePreLock createOfflinePreLock(OfflinePreLockDTO dto) {
        OfflinePreLock record = new OfflinePreLock();
        BeanUtils.copyProperties(dto, record);

        if (dto.getCouponIds() != null && !dto.getCouponIds().isEmpty()) {
            try {
                record.setCouponIds(objectMapper.writeValueAsString(dto.getCouponIds()));
            } catch (Exception e) {
                log.warn("序列化券ID列表失败", e);
            }
        }

        record.setSyncStatus(SYNC_STATUS_PENDING);
        record.setSyncRetryCount(0);
        record.setIsIdempotent(false);

        if (dto.getPreLockTime() == null) {
            record.setPreLockTime(LocalDateTime.now());
        }

        offlinePreLockMapper.insert(record);
        return record;
    }

    private OfflineSyncResultVO doSync(OfflinePreLock record, OfflinePreLockDTO dto) {
        String offlineLockNo = record.getOfflineLockNo();
        String orderNo = record.getOrderNo();

        if (orderNo == null || orderNo.isEmpty()) {
            orderNo = offlineLockNo;
            record.setOrderNo(orderNo);
        }

        Integer benefitType = record.getBenefitType();
        if (benefitType == null) {
            benefitType = 3;
        }

        Long memberId = record.getMemberId();
        if (memberId == null) {
            throw new BusinessException("会员ID不能为空");
        }

        BenefitLockDTO lockDTO = new BenefitLockDTO();
        lockDTO.setOrderNo(orderNo);
        lockDTO.setMemberId(memberId);
        lockDTO.setBenefitType(benefitType);
        lockDTO.setOrderAmount(record.getOrderAmount());
        lockDTO.setUsedPoints(record.getUsedPoints());
        lockDTO.setStoreCode(record.getStoreCode());
        lockDTO.setPosCode(record.getPosCode());
        lockDTO.setOperator(record.getCashier());

        if (dto.getCouponIds() != null && !dto.getCouponIds().isEmpty()) {
            lockDTO.setBenefitId(dto.getCouponIds());
        }

        BenefitLockResultVO lockResult = benefitService.lockBenefits(lockDTO);

        BenefitConfirmDTO confirmDTO = new BenefitConfirmDTO();
        confirmDTO.setOrderNo(orderNo);
        confirmDTO.setMemberId(memberId);
        confirmDTO.setOrderAmount(record.getOrderAmount());
        confirmDTO.setStoreCode(record.getStoreCode());
        confirmDTO.setPosCode(record.getPosCode());
        confirmDTO.setOperator(record.getCashier());

        if (lockResult.getUseNo() != null) {
            confirmDTO.setUseNo(lockResult.getUseNo());
        }

        BenefitConfirmResultVO confirmResult = benefitService.confirmBenefits(confirmDTO);

        record.setSyncStatus(SYNC_STATUS_SUCCESS);
        record.setSyncTime(LocalDateTime.now());
        record.setOrderNo(orderNo);
        record.setUseNo(confirmResult.getUseNo());
        record.setIsIdempotent(false);
        offlinePreLockMapper.updateById(record);

        OfflineSyncResultVO result = new OfflineSyncResultVO();
        result.setOfflineLockNo(offlineLockNo);
        result.setSyncStatus(SYNC_STATUS_SUCCESS);
        result.setSyncStatusName("同步成功");
        result.setIsIdempotent(false);
        result.setOrderNo(orderNo);
        result.setUseNo(confirmResult.getUseNo());
        result.setMessage("同步成功");

        return result;
    }

    private OfflineSyncResultVO buildIdempotentResult(OfflinePreLock record) {
        OfflineSyncResultVO result = new OfflineSyncResultVO();
        result.setOfflineLockNo(record.getOfflineLockNo());
        result.setSyncStatus(SYNC_STATUS_SUCCESS);
        result.setSyncStatusName("同步成功");
        result.setIsIdempotent(true);
        result.setOrderNo(record.getOrderNo());
        result.setUseNo(record.getUseNo());
        result.setMessage("重复提交，幂等返回");
        return result;
    }

    private OfflinePreLockVO convertToVO(OfflinePreLock entity) {
        OfflinePreLockVO vo = new OfflinePreLockVO();
        BeanUtils.copyProperties(entity, vo);

        vo.setSyncStatusName(getSyncStatusName(entity.getSyncStatus()));
        vo.setBenefitTypeName(getBenefitTypeName(entity.getBenefitType()));

        return vo;
    }

    private String getSyncStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待同步";
            case 1 -> "同步中";
            case 2 -> "同步成功";
            case 3 -> "同步失败";
            default -> "未知";
        };
    }

    private String getBenefitTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "优惠券";
            case 2 -> "积分";
            case 3 -> "组合";
            default -> "未知";
        };
    }
}
