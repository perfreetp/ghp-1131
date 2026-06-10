package com.smartretail.mbc.point.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.enums.PointSourceEnum;
import com.smartretail.mbc.common.enums.PointTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.point.dto.PointAddDTO;
import com.smartretail.mbc.point.dto.PointFreezeDTO;
import com.smartretail.mbc.point.dto.PointQueryDTO;
import com.smartretail.mbc.point.dto.PointRefundReturnDTO;
import com.smartretail.mbc.point.dto.PointSubtractDTO;
import com.smartretail.mbc.point.dto.PointUnfreezeDTO;
import com.smartretail.mbc.point.entity.PointLog;
import com.smartretail.mbc.point.mapper.PointLogMapper;
import com.smartretail.mbc.point.service.PointService;
import com.smartretail.mbc.point.vo.PointAccountVO;
import com.smartretail.mbc.point.vo.PointChangeResultVO;
import com.smartretail.mbc.point.vo.PointLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointLogMapper pointLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberMapper memberMapper;

    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final String REFUND_IDEMPOTENT_PREFIX = "mbc:point:refund:";
    private static final long REFUND_IDEMPOTENT_HOURS = 24;

    @Override
    public PointAccountVO getAccountInfo(Long memberId) {
        if (memberId == null) {
            throw new BusinessException("会员ID不能为空");
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        PointAccountVO vo = new PointAccountVO();
        vo.setMemberId(memberId);
        vo.setCurrentPoints(member.getCurrentPoints() != null ? member.getCurrentPoints() : 0);
        vo.setFrozenPoints(0);

        LambdaQueryWrapper<PointLog> freezeWrapper = new LambdaQueryWrapper<>();
        freezeWrapper.eq(PointLog::getMemberId, memberId)
                .eq(PointLog::getPointType, PointTypeEnum.FREEZE.getCode())
                .orderByDesc(PointLog::getCreateTime);
        List<PointLog> freezeLogs = pointLogMapper.selectList(freezeWrapper);
        int totalFreeze = freezeLogs.stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
        int totalUnfreeze = 0;
        LambdaQueryWrapper<PointLog> unfreezeWrapper = new LambdaQueryWrapper<>();
        unfreezeWrapper.eq(PointLog::getMemberId, memberId)
                .eq(PointLog::getPointType, PointTypeEnum.UNFREEZE.getCode());
        List<PointLog> unfreezeLogs = pointLogMapper.selectList(unfreezeWrapper);
        totalUnfreeze = unfreezeLogs.stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
        vo.setFrozenPoints(Math.max(0, totalFreeze - totalUnfreeze));

        int expiringSoon = countExpiringInDays(memberId, 30);
        vo.setExpiringSoonPoints(expiringSoon);

        List<PointLog> expiringLogs = pointLogMapper.selectExpiringPoints(memberId, 365);
        Map<LocalDate, Integer> expireMap = new HashMap<>();
        if (expiringLogs != null) {
            for (PointLog pl : expiringLogs) {
                if (pl.getExpireTime() != null) {
                    LocalDate date = pl.getExpireTime().toLocalDate();
                    expireMap.merge(date, pl.getChangePoints(), Integer::sum);
                }
            }
        }
        vo.setExpireTimeMap(expireMap);

        Integer totalEarned = pointLogMapper.selectTotalByType(memberId, PointTypeEnum.ADD.getCode());
        vo.setTotalEarned(totalEarned != null ? totalEarned : 0);

        Integer totalUsed = pointLogMapper.selectTotalByType(memberId, PointTypeEnum.SUBTRACT.getCode());
        vo.setTotalUsed(totalUsed != null ? totalUsed : 0);

        return vo;
    }

    @Override
    public IPage<PointLogVO> queryLogs(PointQueryDTO dto) {
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(20);
        }
        Page<PointLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<>();
        if (dto.getMemberId() != null) {
            wrapper.eq(PointLog::getMemberId, dto.getMemberId());
        }
        if (dto.getPointType() != null) {
            wrapper.eq(PointLog::getPointType, dto.getPointType());
        }
        if (dto.getSourceType() != null) {
            wrapper.eq(PointLog::getSourceType, dto.getSourceType());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(PointLog::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(PointLog::getCreateTime, dto.getEndTime());
        }
        wrapper.orderByDesc(PointLog::getCreateTime);

        IPage<PointLog> logPage = pointLogMapper.selectPage(page, wrapper);
        return logPage.convert(this::convertToLogVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointChangeResultVO addPoints(PointAddDTO dto) {
        String lockKey = RedisKeyUtil.pointLock(dto.getMemberId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("积分操作进行中，请稍后重试");
        }
        try {
            Member member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                throw new BusinessException("会员不存在");
            }
            int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
            int totalBefore = member.getTotalPoints() != null ? member.getTotalPoints() : 0;
            int afterPoints = beforePoints + dto.getPoints();
            int totalAfter = totalBefore + dto.getPoints();

            member.setCurrentPoints(afterPoints);
            member.setTotalPoints(totalAfter);
            memberMapper.updateById(member);

            int expireDays = dto.getExpireDays() != null ? dto.getExpireDays() : 365;
            LocalDateTime expireTime = LocalDateTime.now().plusDays(expireDays);

            PointLog pointLog = new PointLog();
            pointLog.setMemberId(dto.getMemberId());
            pointLog.setPointType(PointTypeEnum.ADD.getCode());
            pointLog.setChangePoints(dto.getPoints());
            pointLog.setBeforePoints(beforePoints);
            pointLog.setAfterPoints(afterPoints);
            pointLog.setFrozenPoints(0);
            pointLog.setSourceType(dto.getSourceType());
            pointLog.setSourceId(dto.getSourceId());
            pointLog.setExpireTime(expireTime);
            pointLog.setRemark(dto.getRemark());
            pointLogMapper.insert(pointLog);

            PointChangeResultVO result = new PointChangeResultVO();
            result.setMemberId(dto.getMemberId());
            result.setBeforePoints(beforePoints);
            result.setAfterPoints(afterPoints);
            result.setChangePoints(dto.getPoints());
            result.setChangeType("ADD");
            result.setLogId(pointLog.getId());
            return result;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointChangeResultVO subtractPoints(PointSubtractDTO dto) {
        String lockKey = RedisKeyUtil.pointLock(dto.getMemberId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("积分操作进行中，请稍后重试");
        }
        try {
            Member member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                throw new BusinessException("会员不存在");
            }
            int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
            if (beforePoints < dto.getPoints()) {
                throw new BusinessException("可用积分不足");
            }
            int afterPoints = beforePoints - dto.getPoints();

            member.setCurrentPoints(afterPoints);
            memberMapper.updateById(member);

            PointLog pointLog = new PointLog();
            pointLog.setMemberId(dto.getMemberId());
            pointLog.setPointType(PointTypeEnum.SUBTRACT.getCode());
            pointLog.setChangePoints(dto.getPoints());
            pointLog.setBeforePoints(beforePoints);
            pointLog.setAfterPoints(afterPoints);
            pointLog.setFrozenPoints(0);
            pointLog.setSourceType(dto.getSourceType());
            pointLog.setSourceId(dto.getSourceId());
            pointLog.setRemark(dto.getRemark());
            pointLogMapper.insert(pointLog);

            PointChangeResultVO result = new PointChangeResultVO();
            result.setMemberId(dto.getMemberId());
            result.setBeforePoints(beforePoints);
            result.setAfterPoints(afterPoints);
            result.setChangePoints(dto.getPoints());
            result.setChangeType("SUBTRACT");
            result.setLogId(pointLog.getId());
            return result;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointChangeResultVO freezePoints(PointFreezeDTO dto) {
        String lockKey = RedisKeyUtil.pointLock(dto.getMemberId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("积分操作进行中，请稍后重试");
        }
        try {
            Member member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                throw new BusinessException("会员不存在");
            }
            int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
            if (beforePoints < dto.getPoints()) {
                throw new BusinessException("可用积分不足，无法冻结");
            }
            int afterPoints = beforePoints - dto.getPoints();

            LambdaQueryWrapper<PointLog> freezeWrapper = new LambdaQueryWrapper<>();
            freezeWrapper.eq(PointLog::getMemberId, dto.getMemberId())
                    .eq(PointLog::getPointType, PointTypeEnum.FREEZE.getCode());
            int totalFreeze = pointLogMapper.selectList(freezeWrapper)
                    .stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
            LambdaQueryWrapper<PointLog> unfreezeWrapper = new LambdaQueryWrapper<>();
            unfreezeWrapper.eq(PointLog::getMemberId, dto.getMemberId())
                    .eq(PointLog::getPointType, PointTypeEnum.UNFREEZE.getCode());
            int totalUnfreeze = pointLogMapper.selectList(unfreezeWrapper)
                    .stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
            int currentFrozen = Math.max(0, totalFreeze - totalUnfreeze);
            int afterFrozen = currentFrozen + dto.getPoints();

            member.setCurrentPoints(afterPoints);
            memberMapper.updateById(member);

            PointLog pointLog = new PointLog();
            pointLog.setMemberId(dto.getMemberId());
            pointLog.setPointType(PointTypeEnum.FREEZE.getCode());
            pointLog.setChangePoints(dto.getPoints());
            pointLog.setBeforePoints(beforePoints);
            pointLog.setAfterPoints(afterPoints);
            pointLog.setFrozenPoints(afterFrozen);
            pointLog.setSourceId(dto.getSourceId());
            pointLog.setRemark(dto.getRemark());
            pointLogMapper.insert(pointLog);

            PointChangeResultVO result = new PointChangeResultVO();
            result.setMemberId(dto.getMemberId());
            result.setBeforePoints(beforePoints);
            result.setAfterPoints(afterPoints);
            result.setChangePoints(dto.getPoints());
            result.setChangeType("FREEZE");
            result.setLogId(pointLog.getId());
            return result;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointChangeResultVO unfreezePoints(PointUnfreezeDTO dto) {
        String lockKey = RedisKeyUtil.pointLock(dto.getMemberId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("积分操作进行中，请稍后重试");
        }
        try {
            Member member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                throw new BusinessException("会员不存在");
            }

            LambdaQueryWrapper<PointLog> freezeWrapper = new LambdaQueryWrapper<>();
            freezeWrapper.eq(PointLog::getMemberId, dto.getMemberId())
                    .eq(PointLog::getPointType, PointTypeEnum.FREEZE.getCode());
            int totalFreeze = pointLogMapper.selectList(freezeWrapper)
                    .stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
            LambdaQueryWrapper<PointLog> unfreezeWrapper = new LambdaQueryWrapper<>();
            unfreezeWrapper.eq(PointLog::getMemberId, dto.getMemberId())
                    .eq(PointLog::getPointType, PointTypeEnum.UNFREEZE.getCode());
            int totalUnfreeze = pointLogMapper.selectList(unfreezeWrapper)
                    .stream().mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();
            int currentFrozen = Math.max(0, totalFreeze - totalUnfreeze);
            if (currentFrozen < dto.getPoints()) {
                throw new BusinessException("冻结积分不足，无法解冻");
            }

            int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
            int afterPoints = beforePoints + dto.getPoints();
            int afterFrozen = currentFrozen - dto.getPoints();

            member.setCurrentPoints(afterPoints);
            memberMapper.updateById(member);

            PointLog pointLog = new PointLog();
            pointLog.setMemberId(dto.getMemberId());
            pointLog.setPointType(PointTypeEnum.UNFREEZE.getCode());
            pointLog.setChangePoints(dto.getPoints());
            pointLog.setBeforePoints(beforePoints);
            pointLog.setAfterPoints(afterPoints);
            pointLog.setFrozenPoints(afterFrozen);
            pointLog.setSourceId(dto.getSourceId());
            pointLog.setRemark(dto.getRemark());
            pointLogMapper.insert(pointLog);

            PointChangeResultVO result = new PointChangeResultVO();
            result.setMemberId(dto.getMemberId());
            result.setBeforePoints(beforePoints);
            result.setAfterPoints(afterPoints);
            result.setChangePoints(dto.getPoints());
            result.setChangeType("UNFREEZE");
            result.setLogId(pointLog.getId());
            return result;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointChangeResultVO refundReturnPoints(PointRefundReturnDTO dto) {
        String idempotentKey = REFUND_IDEMPOTENT_PREFIX + dto.getRefundOrderNo();
        Boolean idempotent = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", REFUND_IDEMPOTENT_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(idempotent)) {
            throw new BusinessException("退款返还已处理，请勿重复提交");
        }

        String lockKey = RedisKeyUtil.pointLock(dto.getMemberId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            stringRedisTemplate.delete(idempotentKey);
            throw new BusinessException("积分操作进行中，请稍后重试");
        }
        try {
            LambdaQueryWrapper<PointLog> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(PointLog::getMemberId, dto.getMemberId())
                    .eq(PointLog::getSourceType, PointSourceEnum.REFUND_RETURN.getCode())
                    .eq(PointLog::getSourceId, dto.getOriginalOrderNo())
                    .eq(PointLog::getPointType, PointTypeEnum.ADD.getCode());
            List<PointLog> existLogs = pointLogMapper.selectList(existWrapper);
            int alreadyReturned = existLogs.stream()
                    .mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0).sum();

            Member member = memberMapper.selectById(dto.getMemberId());
            if (member == null) {
                throw new BusinessException("会员不存在");
            }

            int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
            int totalBefore = member.getTotalPoints() != null ? member.getTotalPoints() : 0;
            int afterPoints = beforePoints + dto.getReturnPoints();
            int totalAfter = totalBefore + dto.getReturnPoints();

            member.setCurrentPoints(afterPoints);
            member.setTotalPoints(totalAfter);
            memberMapper.updateById(member);

            LocalDateTime expireTime = LocalDateTime.now().plusDays(365);

            String remark = dto.getReason();
            if (!StringUtils.hasText(remark)) {
                remark = "退款返还，原订单号：" + dto.getOriginalOrderNo() + "，已返还：" + alreadyReturned + "，本次返还：" + dto.getReturnPoints();
            }

            PointLog pointLog = new PointLog();
            pointLog.setMemberId(dto.getMemberId());
            pointLog.setPointType(PointTypeEnum.ADD.getCode());
            pointLog.setChangePoints(dto.getReturnPoints());
            pointLog.setBeforePoints(beforePoints);
            pointLog.setAfterPoints(afterPoints);
            pointLog.setFrozenPoints(0);
            pointLog.setSourceType(PointSourceEnum.REFUND_RETURN.getCode());
            pointLog.setSourceId(dto.getOriginalOrderNo());
            pointLog.setExpireTime(expireTime);
            pointLog.setRemark(remark);
            pointLogMapper.insert(pointLog);

            PointChangeResultVO result = new PointChangeResultVO();
            result.setMemberId(dto.getMemberId());
            result.setBeforePoints(beforePoints);
            result.setAfterPoints(afterPoints);
            result.setChangePoints(dto.getReturnPoints());
            result.setChangeType("ADD");
            result.setLogId(pointLog.getId());
            return result;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void expirePointsTask() {
        log.info("[积分过期定时任务] 开始执行...");
        try {
            LambdaQueryWrapper<PointLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PointLog::getPointType, PointTypeEnum.ADD.getCode())
                    .isNotNull(PointLog::getExpireTime)
                    .lt(PointLog::getExpireTime, LocalDateTime.now())
                    .orderByAsc(PointLog::getMemberId, PointLog::getExpireTime);
            List<PointLog> expiredLogs = pointLogMapper.selectList(queryWrapper);

            if (expiredLogs == null || expiredLogs.isEmpty()) {
                log.info("[积分过期定时任务] 无过期积分，任务结束");
                return;
            }

            Map<Long, List<PointLog>> groupedByMember = expiredLogs.stream()
                    .collect(Collectors.groupingBy(PointLog::getMemberId));

            int successCount = 0;
            for (Map.Entry<Long, List<PointLog>> entry : groupedByMember.entrySet()) {
                Long memberId = entry.getKey();
                List<PointLog> memberLogs = entry.getValue();

                String lockKey = RedisKeyUtil.pointLock(memberId);
                String lockValue = UUID.randomUUID().toString();
                Boolean locked = stringRedisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(locked)) {
                    log.warn("[积分过期定时任务] 获取会员锁失败，memberId={}，跳过", memberId);
                    continue;
                }
                try {
                    Member member = memberMapper.selectById(memberId);
                    if (member == null) {
                        continue;
                    }

                    int totalExpiredPoints = 0;
                    List<Long> processedLogIds = new ArrayList<>();

                    for (PointLog addLog : memberLogs) {
                        LambdaQueryWrapper<PointLog> usedWrapper = new LambdaQueryWrapper<>();
                        usedWrapper.eq(PointLog::getMemberId, memberId)
                                .eq(PointLog::getPointType, PointTypeEnum.SUBTRACT.getCode())
                                .le(PointLog::getCreateTime, LocalDateTime.now())
                                .orderByAsc(PointLog::getCreateTime);
                        totalExpiredPoints += addLog.getChangePoints() != null ? addLog.getChangePoints() : 0;
                        processedLogIds.add(addLog.getId());
                    }

                    if (totalExpiredPoints <= 0) {
                        continue;
                    }

                    int beforePoints = member.getCurrentPoints() != null ? member.getCurrentPoints() : 0;
                    int actualExpire = Math.min(beforePoints, totalExpiredPoints);
                    if (actualExpire <= 0) {
                        continue;
                    }
                    int afterPoints = beforePoints - actualExpire;

                    member.setCurrentPoints(afterPoints);
                    memberMapper.updateById(member);

                    PointLog expireLog = new PointLog();
                    expireLog.setMemberId(memberId);
                    expireLog.setPointType(PointTypeEnum.SUBTRACT.getCode());
                    expireLog.setChangePoints(actualExpire);
                    expireLog.setBeforePoints(beforePoints);
                    expireLog.setAfterPoints(afterPoints);
                    expireLog.setFrozenPoints(0);
                    expireLog.setRemark("积分过期自动扣减，过期批次数量：" + processedLogIds.size());
                    pointLogMapper.insert(expireLog);

                    successCount++;
                } catch (Exception e) {
                    log.error("[积分过期定时任务] 处理会员积分过期异常，memberId={}", memberId, e);
                } finally {
                    releaseLock(lockKey, lockValue);
                }
            }

            log.info("[积分过期定时任务] 执行完成，成功处理会员数：{}", successCount);
        } catch (Exception e) {
            log.error("[积分过期定时任务] 执行异常", e);
        }
    }

    @Override
    public int countExpiringInDays(Long memberId, int days) {
        if (memberId == null || days <= 0) {
            return 0;
        }
        List<PointLog> logs = pointLogMapper.selectExpiringPoints(memberId, days);
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return logs.stream()
                .mapToInt(l -> l.getChangePoints() != null ? l.getChangePoints() : 0)
                .sum();
    }

    private PointLogVO convertToLogVO(PointLog pointLog) {
        if (pointLog == null) {
            return null;
        }
        PointLogVO vo = new PointLogVO();
        vo.setId(pointLog.getId());
        vo.setMemberId(pointLog.getMemberId());
        vo.setPointType(pointLog.getPointType());
        vo.setChangePoints(pointLog.getChangePoints());
        vo.setBeforePoints(pointLog.getBeforePoints());
        vo.setAfterPoints(pointLog.getAfterPoints());
        vo.setFrozenPoints(pointLog.getFrozenPoints());
        vo.setSourceType(pointLog.getSourceType());
        vo.setSourceId(pointLog.getSourceId());
        vo.setExpireTime(pointLog.getExpireTime());
        vo.setRemark(pointLog.getRemark());
        vo.setCreateTime(pointLog.getCreateTime());
        vo.setCreateBy(pointLog.getCreateBy());

        String pointTypeName = null;
        for (PointTypeEnum e : PointTypeEnum.values()) {
            if (e.getCode().equals(pointLog.getPointType())) {
                pointTypeName = e.getName();
                break;
            }
        }
        vo.setPointTypeName(pointTypeName);

        String sourceTypeName = null;
        if (pointLog.getSourceType() != null) {
            for (PointSourceEnum e : PointSourceEnum.values()) {
                if (e.getCode().equals(pointLog.getSourceType())) {
                    sourceTypeName = e.getName();
                    break;
                }
            }
        }
        vo.setSourceTypeName(sourceTypeName);

        return vo;
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.warn("释放积分锁失败，key={}", lockKey, e);
        }
    }
}
