package com.smartretail.mbc.benefit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.dto.BenefitConfirmDTO;
import com.smartretail.mbc.benefit.dto.BenefitLockDTO;
import com.smartretail.mbc.benefit.dto.BenefitQueryDTO;
import com.smartretail.mbc.benefit.dto.BenefitReturnDTO;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.service.BenefitService;
import com.smartretail.mbc.benefit.vo.BenefitConfirmResultVO;
import com.smartretail.mbc.benefit.vo.BenefitLockResultVO;
import com.smartretail.mbc.benefit.vo.BenefitUseVO;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.point.dto.PointFreezeDTO;
import com.smartretail.mbc.point.dto.PointRefundReturnDTO;
import com.smartretail.mbc.point.dto.PointSubtractDTO;
import com.smartretail.mbc.point.dto.PointUnfreezeDTO;
import com.smartretail.mbc.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitServiceImpl implements BenefitService {

    private final BenefitUseLogMapper benefitUseLogMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final MemberMapper memberMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Autowired
    private PointService pointService;

    private static final int BENEFIT_TYPE_COUPON = 1;
    private static final int BENEFIT_TYPE_POINT = 2;
    private static final int BENEFIT_TYPE_LEVEL = 3;
    private static final int BENEFIT_TYPE_EXCHANGE = 4;

    private static final int USE_STATUS_LOCKED = 1;
    private static final int USE_STATUS_CONFIRMED = 2;
    private static final int USE_STATUS_RETURNED = 3;

    private static final int POINTS_PER_YUAN = 100;
    private static final int LOCK_EXPIRE_MINUTES = 30;
    private static final String USE_NO_PREFIX = "BU";

    private static final int PROCESS_STATUS_PROCESSING = 1;
    private static final int PROCESS_STATUS_COMPLETED = 2;
    private static final int PROCESS_STATUS_FAILED = 3;

    private final Map<Integer, BigDecimal> levelDiscountMap = new HashMap<>();

    {
        levelDiscountMap.put(MemberLevelEnum.BRONZE.getCode(), new BigDecimal("10.0"));
        levelDiscountMap.put(MemberLevelEnum.SILVER.getCode(), new BigDecimal("9.8"));
        levelDiscountMap.put(MemberLevelEnum.GOLD.getCode(), new BigDecimal("9.5"));
        levelDiscountMap.put(MemberLevelEnum.PLATINUM.getCode(), new BigDecimal("9.0"));
        levelDiscountMap.put(MemberLevelEnum.DIAMOND.getCode(), new BigDecimal("8.5"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitLockResultVO lockBenefits(BenefitLockDTO dto) {
        String requestId = UUID.randomUUID().toString();
        String idemLockKey = RedisKeyUtil.idemBenefitLock(dto.getOrderNo()) + ":" + dto.getBenefitType();
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(idemLockKey, lockValue, 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo())
                    .eq(BenefitUseLog::getBenefitType, dto.getBenefitType())
                    .eq(BenefitUseLog::getUseStatus, USE_STATUS_LOCKED);
            List<BenefitUseLog> existLogs = benefitUseLogMapper.selectList(queryWrapper);
            if (!CollectionUtils.isEmpty(existLogs)) {
                return buildLockIdempotentResult(existLogs, requestId);
            }
            BenefitLockResultVO result = new BenefitLockResultVO();
            result.setIdempotent(false);
            result.setRequestId(requestId);
            result.setProcessStatus(PROCESS_STATUS_PROCESSING);
            result.setBenefitType(dto.getBenefitType());
            return result;
        }
        try {
            LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo())
                    .eq(BenefitUseLog::getBenefitType, dto.getBenefitType())
                    .eq(BenefitUseLog::getUseStatus, USE_STATUS_LOCKED);
            List<BenefitUseLog> existLogs = benefitUseLogMapper.selectList(queryWrapper);
            if (!CollectionUtils.isEmpty(existLogs)) {
                return buildLockIdempotentResult(existLogs, requestId);
            }

            BenefitLockResultVO result = doLockBenefits(dto);
            result.setIdempotent(false);
            result.setRequestId(requestId);
            result.setProcessStatus(PROCESS_STATUS_COMPLETED);
            return result;
        } finally {
            String currentValue = stringRedisTemplate.opsForValue().get(idemLockKey);
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(idemLockKey);
            }
        }
    }

    private BenefitLockResultVO buildLockIdempotentResult(List<BenefitUseLog> logs, String requestId) {
        BenefitLockResultVO result = new BenefitLockResultVO();
        BenefitUseLog firstLog = logs.get(0);
        result.setUseNo(firstLog.getUseNo());
        result.setBenefitType(firstLog.getBenefitType());
        result.setIdempotent(true);
        result.setRequestId(requestId);
        result.setProcessStatus(PROCESS_STATUS_COMPLETED);

        BigDecimal totalBenefitValue = BigDecimal.ZERO;
        List<Long> couponIds = new ArrayList<>();
        Integer usedPoints = 0;
        BigDecimal levelDiscountSaved = BigDecimal.ZERO;

        for (BenefitUseLog log : logs) {
            if (log.getBenefitValue() != null) {
                totalBenefitValue = totalBenefitValue.add(log.getBenefitValue());
            }
            if ((BENEFIT_TYPE_COUPON == log.getBenefitType() || BENEFIT_TYPE_EXCHANGE == log.getBenefitType())
                    && log.getBenefitId() != null) {
                couponIds.add(log.getBenefitId());
            }
            if (BENEFIT_TYPE_POINT == log.getBenefitType() && log.getUsedPoints() != null) {
                usedPoints = log.getUsedPoints();
            }
            if (BENEFIT_TYPE_LEVEL == log.getBenefitType() && log.getBenefitValue() != null) {
                levelDiscountSaved = log.getBenefitValue();
            }
        }

        result.setBenefitValue(totalBenefitValue);
        if (!couponIds.isEmpty()) {
            result.setReducedCouponIds(couponIds);
        }
        if (usedPoints > 0) {
            result.setUsedPoints(usedPoints);
        }
        if (levelDiscountSaved.compareTo(BigDecimal.ZERO) > 0) {
            result.setLevelDiscountSaved(levelDiscountSaved);
        }

        return result;
    }

    private BenefitLockResultVO doLockBenefits(BenefitLockDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        Integer benefitType = dto.getBenefitType();
        BenefitLockResultVO result = new BenefitLockResultVO();
        result.setBenefitType(benefitType);
        result.setUseNo(generateUseNo());

        LocalDateTime now = LocalDateTime.now();

        if (BENEFIT_TYPE_COUPON == benefitType) {
            List<Long> couponIds = dto.getBenefitId();
            if (CollectionUtils.isEmpty(couponIds)) {
                throw new BusinessException("优惠券类型必须指定券ID");
            }
            List<Long> lockedCouponIds = new ArrayList<>();
            BigDecimal totalBenefitValue = BigDecimal.ZERO;

            for (Long couponId : couponIds) {
                CouponInstance instance = couponInstanceMapper.selectById(couponId);
                if (instance == null) {
                    throw new BusinessException("券实例不存在: " + couponId);
                }
                if (!CouponStatusEnum.AVAILABLE.getCode().equals(instance.getCouponStatus())) {
                    throw new BusinessException("券状态不可用: " + couponId);
                }
                if (!member.getId().equals(instance.getMemberId())) {
                    throw new BusinessException("券不属于该会员: " + couponId);
                }
                if (instance.getValidEnd() != null && instance.getValidEnd().isBefore(now)) {
                    throw new BusinessException("券已过期: " + couponId);
                }
                CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
                if (template != null && template.getFullAmount() != null
                        && dto.getOrderAmount().compareTo(template.getFullAmount()) < 0) {
                    throw new BusinessException("订单金额未达到券使用门槛: " + couponId);
                }

                LambdaUpdateWrapper<CouponInstance> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(CouponInstance::getId, couponId)
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode())
                        .set(CouponInstance::getCouponStatus, CouponStatusEnum.LOCKED.getCode())
                        .set(CouponInstance::getLockOrderNo, dto.getOrderNo())
                        .set(CouponInstance::getLockedTime, now);
                int updated = couponInstanceMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    throw new BusinessException("券锁定失败，可能已被其他操作占用: " + couponId);
                }

                BigDecimal reduceAmount = template != null && template.getReduceAmount() != null
                        ? template.getReduceAmount() : BigDecimal.ZERO;
                totalBenefitValue = totalBenefitValue.add(reduceAmount);
                lockedCouponIds.add(couponId);

                BenefitUseLog useLog = new BenefitUseLog();
                useLog.setUseNo(result.getUseNo());
                useLog.setMemberId(dto.getMemberId());
                useLog.setBenefitType(BENEFIT_TYPE_COUPON);
                useLog.setBenefitId(couponId);
                useLog.setUseStatus(USE_STATUS_LOCKED);
                useLog.setOrderNo(dto.getOrderNo());
                useLog.setOrderAmount(dto.getOrderAmount());
                useLog.setBenefitValue(reduceAmount);
                useLog.setStoreCode(dto.getStoreCode());
                useLog.setPosCode(dto.getPosCode());
                useLog.setOperator(dto.getOperator());
                useLog.setLockTime(now);
                benefitUseLogMapper.insert(useLog);
            }

            result.setBenefitValue(totalBenefitValue);
            result.setReducedCouponIds(lockedCouponIds);

        } else if (BENEFIT_TYPE_POINT == benefitType) {
            Integer usedPoints = dto.getUsedPoints();
            if (usedPoints == null || usedPoints <= 0) {
                throw new BusinessException("积分抵扣类型必须指定使用积分数");
            }
            if (member.getCurrentPoints() == null || member.getCurrentPoints() < usedPoints) {
                throw new BusinessException("会员可用积分不足");
            }

            PointFreezeDTO freezeDTO = new PointFreezeDTO();
            freezeDTO.setMemberId(dto.getMemberId());
            freezeDTO.setPoints(usedPoints);
            freezeDTO.setSourceId(dto.getOrderNo());
            freezeDTO.setRemark("订单积分抵扣冻结");
            pointService.freezePoints(freezeDTO);

            BigDecimal benefitValue = new BigDecimal(usedPoints)
                    .divide(new BigDecimal(POINTS_PER_YUAN), 2, RoundingMode.HALF_UP);

            BenefitUseLog useLog = new BenefitUseLog();
            useLog.setUseNo(result.getUseNo());
            useLog.setMemberId(dto.getMemberId());
            useLog.setBenefitType(BENEFIT_TYPE_POINT);
            useLog.setUseStatus(USE_STATUS_LOCKED);
            useLog.setOrderNo(dto.getOrderNo());
            useLog.setOrderAmount(dto.getOrderAmount());
            useLog.setBenefitValue(benefitValue);
            useLog.setUsedPoints(usedPoints);
            useLog.setStoreCode(dto.getStoreCode());
            useLog.setPosCode(dto.getPosCode());
            useLog.setOperator(dto.getOperator());
            useLog.setLockTime(now);
            benefitUseLogMapper.insert(useLog);

            result.setBenefitValue(benefitValue);
            result.setUsedPoints(usedPoints);

        } else if (BENEFIT_TYPE_LEVEL == benefitType) {
            Integer levelCode = member.getLevelCode() != null ? member.getLevelCode() : MemberLevelEnum.BRONZE.getCode();
            BigDecimal discountRate = levelDiscountMap.getOrDefault(levelCode, new BigDecimal("10.0"));
            BigDecimal levelDiscountSaved = dto.getOrderAmount()
                    .multiply(new BigDecimal("10").subtract(discountRate))
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);

            BenefitUseLog useLog = new BenefitUseLog();
            useLog.setUseNo(result.getUseNo());
            useLog.setMemberId(dto.getMemberId());
            useLog.setBenefitType(BENEFIT_TYPE_LEVEL);
            useLog.setUseStatus(USE_STATUS_LOCKED);
            useLog.setOrderNo(dto.getOrderNo());
            useLog.setOrderAmount(dto.getOrderAmount());
            useLog.setBenefitValue(levelDiscountSaved);
            useLog.setStoreCode(dto.getStoreCode());
            useLog.setPosCode(dto.getPosCode());
            useLog.setOperator(dto.getOperator());
            useLog.setLockTime(now);
            benefitUseLogMapper.insert(useLog);

            result.setBenefitValue(levelDiscountSaved);
            result.setLevelDiscountSaved(levelDiscountSaved);

        } else if (BENEFIT_TYPE_EXCHANGE == benefitType) {
            List<Long> couponIds = dto.getBenefitId();
            if (CollectionUtils.isEmpty(couponIds)) {
                throw new BusinessException("兑换权益类型必须指定券ID");
            }
            List<Long> lockedCouponIds = new ArrayList<>();

            for (Long couponId : couponIds) {
                CouponInstance instance = couponInstanceMapper.selectById(couponId);
                if (instance == null) {
                    throw new BusinessException("兑换券实例不存在: " + couponId);
                }
                if (!CouponStatusEnum.AVAILABLE.getCode().equals(instance.getCouponStatus())) {
                    throw new BusinessException("兑换券状态不可用: " + couponId);
                }
                if (!member.getId().equals(instance.getMemberId())) {
                    throw new BusinessException("兑换券不属于该会员: " + couponId);
                }

                LambdaUpdateWrapper<CouponInstance> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(CouponInstance::getId, couponId)
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode())
                        .set(CouponInstance::getCouponStatus, CouponStatusEnum.LOCKED.getCode())
                        .set(CouponInstance::getLockOrderNo, dto.getOrderNo())
                        .set(CouponInstance::getLockedTime, now);
                int updated = couponInstanceMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    throw new BusinessException("兑换券锁定失败，可能已被其他操作占用: " + couponId);
                }

                lockedCouponIds.add(couponId);

                BenefitUseLog useLog = new BenefitUseLog();
                useLog.setUseNo(result.getUseNo());
                useLog.setMemberId(dto.getMemberId());
                useLog.setBenefitType(BENEFIT_TYPE_EXCHANGE);
                useLog.setBenefitId(couponId);
                useLog.setUseStatus(USE_STATUS_LOCKED);
                useLog.setOrderNo(dto.getOrderNo());
                useLog.setOrderAmount(dto.getOrderAmount());
                useLog.setBenefitValue(BigDecimal.ZERO);
                useLog.setStoreCode(dto.getStoreCode());
                useLog.setPosCode(dto.getPosCode());
                useLog.setOperator(dto.getOperator());
                useLog.setLockTime(now);
                benefitUseLogMapper.insert(useLog);
            }

            result.setReducedCouponIds(lockedCouponIds);
            result.setBenefitValue(BigDecimal.ZERO);

        } else {
            throw new BusinessException("不支持的权益类型");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitConfirmResultVO confirmBenefits(BenefitConfirmDTO dto) {
        String requestId = UUID.randomUUID().toString();
        String idemKey = RedisKeyUtil.idemBenefitConfirm(dto.getOrderNo() != null ? dto.getOrderNo() : dto.getUseNo());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, lockValue, 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            LambdaQueryWrapper<BenefitUseLog> queryWrapper = buildConfirmQueryWrapper(dto);
            queryWrapper.eq(BenefitUseLog::getUseStatus, USE_STATUS_CONFIRMED);
            List<BenefitUseLog> confirmedLogs = benefitUseLogMapper.selectList(queryWrapper);
            if (!CollectionUtils.isEmpty(confirmedLogs)) {
                return buildConfirmIdempotentResult(confirmedLogs, requestId);
            }
            BenefitConfirmResultVO result = new BenefitConfirmResultVO();
            result.setIdempotent(false);
            result.setRequestId(requestId);
            result.setProcessStatus(PROCESS_STATUS_PROCESSING);
            return result;
        }
        try {
            LambdaQueryWrapper<BenefitUseLog> confirmedQueryWrapper = buildConfirmQueryWrapper(dto);
            confirmedQueryWrapper.eq(BenefitUseLog::getUseStatus, USE_STATUS_CONFIRMED);
            List<BenefitUseLog> confirmedLogs = benefitUseLogMapper.selectList(confirmedQueryWrapper);
            if (!CollectionUtils.isEmpty(confirmedLogs)) {
                return buildConfirmIdempotentResult(confirmedLogs, requestId);
            }

            LambdaQueryWrapper<BenefitUseLog> returnedQueryWrapper = buildConfirmQueryWrapper(dto);
            returnedQueryWrapper.eq(BenefitUseLog::getUseStatus, USE_STATUS_RETURNED);
            List<BenefitUseLog> returnedLogs = benefitUseLogMapper.selectList(returnedQueryWrapper);
            if (!CollectionUtils.isEmpty(returnedLogs)) {
                throw new BusinessException("已退还的权益不能再确认");
            }

            BenefitConfirmResultVO result = doConfirmBenefits(dto);
            result.setIdempotent(false);
            result.setRequestId(requestId);
            result.setProcessStatus(PROCESS_STATUS_COMPLETED);
            return result;
        } finally {
            String currentValue = stringRedisTemplate.opsForValue().get(idemKey);
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(idemKey);
            }
        }
    }

    private LambdaQueryWrapper<BenefitUseLog> buildConfirmQueryWrapper(BenefitConfirmDTO dto) {
        LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BenefitUseLog::getMemberId, dto.getMemberId());
        if (StringUtils.hasText(dto.getUseNo())) {
            queryWrapper.eq(BenefitUseLog::getUseNo, dto.getUseNo());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo());
        }
        return queryWrapper;
    }

    private BenefitConfirmResultVO buildConfirmIdempotentResult(List<BenefitUseLog> logs, String requestId) {
        BenefitConfirmResultVO result = new BenefitConfirmResultVO();
        result.setUseNo(logs.get(0).getUseNo());
        result.setOrderNo(logs.get(0).getOrderNo());
        result.setConfirmed(true);
        result.setDetailList(logs);
        result.setIdempotent(true);
        result.setRequestId(requestId);
        result.setProcessStatus(PROCESS_STATUS_COMPLETED);

        BigDecimal totalBenefitValue = BigDecimal.ZERO;
        for (BenefitUseLog log : logs) {
            if (log.getBenefitValue() != null) {
                totalBenefitValue = totalBenefitValue.add(log.getBenefitValue());
            }
        }
        result.setTotalBenefitValue(totalBenefitValue);
        return result;
    }

    private BenefitConfirmResultVO doConfirmBenefits(BenefitConfirmDTO dto) {
        LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BenefitUseLog::getMemberId, dto.getMemberId())
                .eq(BenefitUseLog::getUseStatus, USE_STATUS_LOCKED);
        if (StringUtils.hasText(dto.getUseNo())) {
            queryWrapper.eq(BenefitUseLog::getUseNo, dto.getUseNo());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo());
        }
        List<BenefitUseLog> logs = benefitUseLogMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(logs)) {
            throw new BusinessException("未找到待确认的锁定记录");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalBenefitValue = BigDecimal.ZERO;

        for (BenefitUseLog useLog : logs) {
            Integer benefitType = useLog.getBenefitType();

            if (BENEFIT_TYPE_COUPON == benefitType || BENEFIT_TYPE_EXCHANGE == benefitType) {
                LambdaUpdateWrapper<CouponInstance> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(CouponInstance::getId, useLog.getBenefitId())
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.LOCKED.getCode())
                        .set(CouponInstance::getCouponStatus, CouponStatusEnum.USED.getCode())
                        .set(CouponInstance::getUsedOrderNo, dto.getOrderNo())
                        .set(CouponInstance::getUsedTime, now);
                int updated = couponInstanceMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    throw new BusinessException("券核销确认失败: " + useLog.getBenefitId());
                }

            } else if (BENEFIT_TYPE_POINT == benefitType) {
                Integer usedPoints = useLog.getUsedPoints();
                if (usedPoints != null && usedPoints > 0) {
                    PointUnfreezeDTO unfreezeDTO = new PointUnfreezeDTO();
                    unfreezeDTO.setMemberId(dto.getMemberId());
                    unfreezeDTO.setPoints(usedPoints);
                    unfreezeDTO.setSourceId(dto.getOrderNo());
                    unfreezeDTO.setRemark("订单积分抵扣确认解冻");
                    pointService.unfreezePoints(unfreezeDTO);

                    PointSubtractDTO subtractDTO = new PointSubtractDTO();
                    subtractDTO.setMemberId(dto.getMemberId());
                    subtractDTO.setPoints(usedPoints);
                    subtractDTO.setSourceId(dto.getOrderNo());
                    subtractDTO.setRemark("订单积分抵扣确认扣减");
                    pointService.subtractPoints(subtractDTO);
                }
            }

            LambdaUpdateWrapper<BenefitUseLog> logUpdateWrapper = new LambdaUpdateWrapper<>();
            logUpdateWrapper.eq(BenefitUseLog::getId, useLog.getId())
                    .eq(BenefitUseLog::getUseStatus, USE_STATUS_LOCKED)
                    .set(BenefitUseLog::getUseStatus, USE_STATUS_CONFIRMED)
                    .set(BenefitUseLog::getConfirmTime, now);
            if (StringUtils.hasText(dto.getStoreCode())) {
                logUpdateWrapper.set(BenefitUseLog::getStoreCode, dto.getStoreCode());
            }
            if (StringUtils.hasText(dto.getPosCode())) {
                logUpdateWrapper.set(BenefitUseLog::getPosCode, dto.getPosCode());
            }
            if (StringUtils.hasText(dto.getOperator())) {
                logUpdateWrapper.set(BenefitUseLog::getOperator, dto.getOperator());
            }
            if (dto.getOrderAmount() != null) {
                logUpdateWrapper.set(BenefitUseLog::getOrderAmount, dto.getOrderAmount());
            }
            benefitUseLogMapper.update(null, logUpdateWrapper);

            useLog.setUseStatus(USE_STATUS_CONFIRMED);
            useLog.setConfirmTime(now);

            if (useLog.getBenefitValue() != null) {
                totalBenefitValue = totalBenefitValue.add(useLog.getBenefitValue());
            }
        }

        BenefitConfirmResultVO result = new BenefitConfirmResultVO();
        result.setUseNo(logs.get(0).getUseNo());
        result.setOrderNo(dto.getOrderNo());
        result.setTotalBenefitValue(totalBenefitValue);
        result.setConfirmed(true);
        result.setDetailList(logs);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BenefitUseVO> returnBenefits(BenefitReturnDTO dto) {
        String requestId = UUID.randomUUID().toString();
        String idemKey = RedisKeyUtil.idemBenefitReturn(dto.getRefundNo());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, lockValue, 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            List<BenefitUseLog> returnedLogs = queryReturnedLogs(dto);
            if (!CollectionUtils.isEmpty(returnedLogs)) {
                return buildReturnIdempotentResult(returnedLogs, requestId);
            }
            List<BenefitUseVO> resultList = new ArrayList<>();
            BenefitUseVO vo = new BenefitUseVO();
            vo.setIdempotent(false);
            vo.setRequestId(requestId);
            vo.setProcessStatus(PROCESS_STATUS_PROCESSING);
            resultList.add(vo);
            return resultList;
        }
        try {
            List<BenefitUseLog> returnedLogs = queryReturnedLogs(dto);
            if (!CollectionUtils.isEmpty(returnedLogs)) {
                return buildReturnIdempotentResult(returnedLogs, requestId);
            }

            List<BenefitUseVO> result = doReturnBenefits(dto);
            for (BenefitUseVO vo : result) {
                vo.setIdempotent(false);
                vo.setRequestId(requestId);
                vo.setProcessStatus(PROCESS_STATUS_COMPLETED);
            }
            return result;
        } finally {
            String currentValue = stringRedisTemplate.opsForValue().get(idemKey);
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(idemKey);
            }
        }
    }

    private List<BenefitUseLog> queryReturnedLogs(BenefitReturnDTO dto) {
        LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BenefitUseLog::getMemberId, dto.getMemberId())
                .eq(BenefitUseLog::getUseStatus, USE_STATUS_RETURNED);
        if (StringUtils.hasText(dto.getUseNo())) {
            queryWrapper.eq(BenefitUseLog::getUseNo, dto.getUseNo());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo());
        }
        return benefitUseLogMapper.selectList(queryWrapper);
    }

    private List<BenefitUseVO> buildReturnIdempotentResult(List<BenefitUseLog> logs, String requestId) {
        List<BenefitUseVO> resultList = new ArrayList<>();
        for (BenefitUseLog log : logs) {
            BenefitUseVO vo = convertToUseVO(log);
            vo.setIdempotent(true);
            vo.setRequestId(requestId);
            vo.setProcessStatus(PROCESS_STATUS_COMPLETED);
            resultList.add(vo);
        }
        return resultList;
    }

    private List<BenefitUseVO> doReturnBenefits(BenefitReturnDTO dto) {
        LambdaQueryWrapper<BenefitUseLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BenefitUseLog::getMemberId, dto.getMemberId())
                .eq(BenefitUseLog::getUseStatus, USE_STATUS_CONFIRMED);
        if (StringUtils.hasText(dto.getUseNo())) {
            queryWrapper.eq(BenefitUseLog::getUseNo, dto.getUseNo());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            queryWrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo());
        }
        List<BenefitUseLog> logs = benefitUseLogMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(logs)) {
            throw new BusinessException("未找到已确认的核销记录");
        }

        LocalDateTime now = LocalDateTime.now();
        List<BenefitUseVO> resultList = new ArrayList<>();

        for (BenefitUseLog useLog : logs) {
            Integer benefitType = useLog.getBenefitType();

            if (BENEFIT_TYPE_COUPON == benefitType || BENEFIT_TYPE_EXCHANGE == benefitType) {
                LambdaUpdateWrapper<CouponInstance> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(CouponInstance::getId, useLog.getBenefitId())
                        .eq(CouponInstance::getCouponStatus, CouponStatusEnum.USED.getCode())
                        .set(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode())
                        .set(CouponInstance::getUsedOrderNo, null)
                        .set(CouponInstance::getUsedTime, null)
                        .set(CouponInstance::getLockOrderNo, null)
                        .set(CouponInstance::getLockedTime, null);
                int updated = couponInstanceMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    throw new BusinessException("券返还失败: " + useLog.getBenefitId());
                }

            } else if (BENEFIT_TYPE_POINT == benefitType) {
                Integer usedPoints = useLog.getUsedPoints();
                if (usedPoints != null && usedPoints > 0) {
                    PointRefundReturnDTO refundDTO = new PointRefundReturnDTO();
                    refundDTO.setRefundOrderNo(dto.getRefundNo());
                    refundDTO.setOriginalOrderNo(useLog.getOrderNo());
                    refundDTO.setMemberId(dto.getMemberId());
                    refundDTO.setReturnPoints(usedPoints);
                    refundDTO.setReason(StringUtils.hasText(dto.getReturnReason()) ? dto.getReturnReason() : "订单退款返还积分");
                    pointService.refundReturnPoints(refundDTO);
                }
            }

            LambdaUpdateWrapper<BenefitUseLog> logUpdateWrapper = new LambdaUpdateWrapper<>();
            logUpdateWrapper.eq(BenefitUseLog::getId, useLog.getId())
                    .eq(BenefitUseLog::getUseStatus, USE_STATUS_CONFIRMED)
                    .set(BenefitUseLog::getUseStatus, USE_STATUS_RETURNED)
                    .set(BenefitUseLog::getReturnTime, now)
                    .set(BenefitUseLog::getReturnReason, dto.getReturnReason());
            if (StringUtils.hasText(dto.getOperator())) {
                logUpdateWrapper.set(BenefitUseLog::getOperator, dto.getOperator());
            }
            benefitUseLogMapper.update(null, logUpdateWrapper);

            BenefitUseVO vo = convertToUseVO(useLog);
            vo.setUseStatus(USE_STATUS_RETURNED);
            vo.setReturnTime(now);
            vo.setReturnReason(dto.getReturnReason());
            vo.setUseStatusName(getUseStatusName(USE_STATUS_RETURNED));
            resultList.add(vo);
        }

        return resultList;
    }

    @Override
    public IPage<BenefitUseVO> queryLogs(BenefitQueryDTO dto) {
        LambdaQueryWrapper<BenefitUseLog> wrapper = new LambdaQueryWrapper<>();
        if (dto.getMemberId() != null) {
            wrapper.eq(BenefitUseLog::getMemberId, dto.getMemberId());
        }
        if (dto.getUseStatus() != null) {
            wrapper.eq(BenefitUseLog::getUseStatus, dto.getUseStatus());
        }
        if (dto.getBenefitType() != null) {
            wrapper.eq(BenefitUseLog::getBenefitType, dto.getBenefitType());
        }
        if (StringUtils.hasText(dto.getOrderNo())) {
            wrapper.eq(BenefitUseLog::getOrderNo, dto.getOrderNo());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(BenefitUseLog::getLockTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(BenefitUseLog::getLockTime, dto.getEndTime());
        }
        wrapper.orderByDesc(BenefitUseLog::getLockTime);

        Page<BenefitUseLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<BenefitUseLog> logPage = benefitUseLogMapper.selectPage(page, wrapper);

        Page<BenefitUseVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        List<BenefitUseVO> voList = logPage.getRecords().stream()
                .map(this::convertToUseVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void releaseExpiredLocksTask() {
        log.info("开始执行权益锁定超时释放任务");
        List<BenefitUseLog> expiredLogs = benefitUseLogMapper.selectLockedWithinMinutes(LOCK_EXPIRE_MINUTES);
        if (CollectionUtils.isEmpty(expiredLogs)) {
            log.info("无超时锁定的权益记录");
            return;
        }

        log.info("发现{}条超时锁定记录，开始释放", expiredLogs.size());
        LocalDateTime now = LocalDateTime.now();

        for (BenefitUseLog useLog : expiredLogs) {
            try {
                Integer benefitType = useLog.getBenefitType();

                if (BENEFIT_TYPE_COUPON == benefitType || BENEFIT_TYPE_EXCHANGE == benefitType) {
                    LambdaUpdateWrapper<CouponInstance> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(CouponInstance::getId, useLog.getBenefitId())
                            .eq(CouponInstance::getCouponStatus, CouponStatusEnum.LOCKED.getCode())
                            .set(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode())
                            .set(CouponInstance::getLockOrderNo, null)
                            .set(CouponInstance::getLockedTime, null);
                    couponInstanceMapper.update(null, updateWrapper);

                } else if (BENEFIT_TYPE_POINT == benefitType) {
                    Integer usedPoints = useLog.getUsedPoints();
                    if (usedPoints != null && usedPoints > 0) {
                        PointUnfreezeDTO unfreezeDTO = new PointUnfreezeDTO();
                        unfreezeDTO.setMemberId(useLog.getMemberId());
                        unfreezeDTO.setPoints(usedPoints);
                        unfreezeDTO.setSourceId(useLog.getOrderNo());
                        unfreezeDTO.setRemark("锁定超时自动解冻");
                        pointService.unfreezePoints(unfreezeDTO);
                    }
                }

                LambdaUpdateWrapper<BenefitUseLog> logUpdateWrapper = new LambdaUpdateWrapper<>();
                logUpdateWrapper.eq(BenefitUseLog::getId, useLog.getId())
                        .eq(BenefitUseLog::getUseStatus, USE_STATUS_LOCKED)
                        .set(BenefitUseLog::getUseStatus, USE_STATUS_RETURNED)
                        .set(BenefitUseLog::getReturnTime, now)
                        .set(BenefitUseLog::getReturnReason, "锁定超时自动释放");
                benefitUseLogMapper.update(null, logUpdateWrapper);

            } catch (Exception e) {
                log.error("释放超时锁定记录失败, logId: {}", useLog.getId(), e);
            }
        }

        log.info("权益锁定超时释放任务执行完成");
    }

    private BenefitUseVO convertToUseVO(BenefitUseLog log) {
        BenefitUseVO vo = new BenefitUseVO();
        vo.setId(log.getId());
        vo.setUseNo(log.getUseNo());
        vo.setMemberId(log.getMemberId());
        vo.setBenefitType(log.getBenefitType());
        vo.setBenefitId(log.getBenefitId());
        vo.setUseStatus(log.getUseStatus());
        vo.setOrderNo(log.getOrderNo());
        vo.setOrderAmount(log.getOrderAmount());
        vo.setBenefitValue(log.getBenefitValue());
        vo.setUsedPoints(log.getUsedPoints());
        vo.setStoreCode(log.getStoreCode());
        vo.setPosCode(log.getPosCode());
        vo.setOperator(log.getOperator());
        vo.setLockTime(log.getLockTime());
        vo.setConfirmTime(log.getConfirmTime());
        vo.setReturnTime(log.getReturnTime());
        vo.setReturnReason(log.getReturnReason());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        vo.setUpdateTime(log.getUpdateTime());
        vo.setBenefitTypeName(getBenefitTypeName(log.getBenefitType()));
        vo.setUseStatusName(getUseStatusName(log.getUseStatus()));

        if ((BENEFIT_TYPE_COUPON == log.getBenefitType() || BENEFIT_TYPE_EXCHANGE == log.getBenefitType())
                && log.getBenefitId() != null) {
            CouponInstance instance = couponInstanceMapper.selectById(log.getBenefitId());
            if (instance != null && instance.getTemplateId() != null) {
                CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
                if (template != null) {
                    vo.setCouponInfo(template.getCouponName());
                }
            }
        }

        return vo;
    }

    private String getBenefitTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case BENEFIT_TYPE_COUPON:
                return "优惠券";
            case BENEFIT_TYPE_POINT:
                return "积分抵扣";
            case BENEFIT_TYPE_LEVEL:
                return "等级折扣";
            case BENEFIT_TYPE_EXCHANGE:
                return "兑换权益";
            default:
                return "未知";
        }
    }

    private String getUseStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case USE_STATUS_LOCKED:
                return "已锁定";
            case USE_STATUS_CONFIRMED:
                return "核销成功";
            case USE_STATUS_RETURNED:
                return "已返还";
            default:
                return "未知";
        }
    }

    private String generateUseNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return USE_NO_PREFIX + datePart + randomPart;
    }
}
