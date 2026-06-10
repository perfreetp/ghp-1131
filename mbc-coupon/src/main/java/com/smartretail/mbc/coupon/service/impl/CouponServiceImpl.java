package com.smartretail.mbc.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.CouponTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.service.BudgetOccupyService;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.coupon.dto.*;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.coupon.service.CouponService;
import com.smartretail.mbc.coupon.vo.*;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberMapper memberMapper;

    @Lazy
    private final BudgetOccupyService budgetOccupyService;

    private static final String INSTANCE_NO_PREFIX = "CI";
    private static final int BATCH_SIZE = 500;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO createTemplate(CouponTemplateCreateDTO dto) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getCouponCode, dto.getCouponCode());
        Long count = couponTemplateMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("券编码已存在");
        }

        if (CouponTypeEnum.FULL_REDUCTION.getCode().equals(dto.getCouponType())) {
            if (dto.getFullAmount() == null || dto.getReduceAmount() == null) {
                throw new BusinessException("满减券必须填写满减金额和减免金额");
            }
            if (dto.getFullAmount().compareTo(dto.getReduceAmount()) <= 0) {
                throw new BusinessException("满减门槛金额必须大于减免金额");
            }
            if (dto.getReduceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("减免金额必须大于0");
            }
        } else if (CouponTypeEnum.EXCHANGE.getCode().equals(dto.getCouponType())) {
            if (!StringUtils.hasText(dto.getExchangeItem())) {
                throw new BusinessException("兑换券必须填写兑换商品");
            }
        } else {
            throw new BusinessException("不支持的券类型");
        }

        if (dto.getValidType() == 1) {
            if (dto.getValidStart() == null || dto.getValidEnd() == null) {
                throw new BusinessException("固定有效期必须填写有效期开始和结束时间");
            }
            if (dto.getValidStart().isAfter(dto.getValidEnd())) {
                throw new BusinessException("有效期开始时间不能晚于结束时间");
            }
        } else if (dto.getValidType() == 2) {
            if (dto.getValidDays() == null || dto.getValidDays() <= 0) {
                throw new BusinessException("领取N天有效必须填写大于0的有效天数");
            }
        } else {
            throw new BusinessException("不支持的有效期类型");
        }

        CouponTemplate template = new CouponTemplate();
        template.setCouponCode(dto.getCouponCode());
        template.setCouponName(dto.getCouponName());
        template.setCouponType(dto.getCouponType());
        template.setTotalAmount(dto.getTotalAmount() == null ? -1 : dto.getTotalAmount());
        template.setReceivedCount(0);
        template.setUsedCount(0);
        template.setFullAmount(dto.getFullAmount());
        template.setReduceAmount(dto.getReduceAmount());
        template.setExchangeItem(dto.getExchangeItem());
        template.setValidType(dto.getValidType());
        template.setValidStart(dto.getValidStart());
        template.setValidEnd(dto.getValidEnd());
        template.setValidDays(dto.getValidDays());
        template.setMinLevel(dto.getMinLevel() == null ? 0 : dto.getMinLevel());
        template.setDailyLimit(dto.getDailyLimit());
        template.setTotalLimit(dto.getTotalLimit());
        template.setApplyScenes(dto.getApplyScenes());
        template.setExcludeItems(dto.getExcludeItems());
        template.setStackable(dto.getStackable() == null ? 0 : dto.getStackable());
        template.setDescription(dto.getDescription());
        template.setStatus(1);
        template.setActivityId(dto.getActivityId());
        couponTemplateMapper.insert(template);

        return convertToTemplateVO(template);
    }

    @Override
    public CouponTemplateVO getTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("券模板不存在");
        }
        return convertToTemplateVO(template);
    }

    @Override
    public IPage<CouponTemplateVO> pageTemplates(CouponTemplateQueryDTO dto) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        if (dto.getCouponType() != null) {
            wrapper.eq(CouponTemplate::getCouponType, dto.getCouponType());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(CouponTemplate::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(CouponTemplate::getCouponName, dto.getKeyword())
                    .or().like(CouponTemplate::getCouponCode, dto.getKeyword()));
        }
        wrapper.orderByDesc(CouponTemplate::getCreateTime);

        Page<CouponTemplate> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<CouponTemplate> templatePage = couponTemplateMapper.selectPage(page, wrapper);

        Page<CouponTemplateVO> voPage = new Page<>(templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());
        List<CouponTemplateVO> voList = templatePage.getRecords().stream()
                .map(this::convertToTemplateVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public CouponReceiveResultVO receiveCoupon(CouponReceiveDTO dto) {
        String lockKey = RedisKeyUtil.couponTemplateLock(dto.getTemplateId());
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            CouponReceiveResultVO result = new CouponReceiveResultVO();
            result.setSuccess(false);
            result.setFailReason("系统繁忙，请稍后重试");
            return result;
        }
        try {
            return doReceiveCoupon(dto);
        } finally {
            String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    private CouponReceiveResultVO doReceiveCoupon(CouponReceiveDTO dto) {
        CouponReceiveResultVO result = new CouponReceiveResultVO();
        result.setSuccess(false);

        CouponTemplate template = couponTemplateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            result.setFailReason("券模板不存在");
            return result;
        }
        if (template.getStatus() == null || template.getStatus() != 1) {
            result.setFailReason("券模板未上架");
            return result;
        }

        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            result.setFailReason("会员不存在");
            return result;
        }
        if (template.getMinLevel() != null && member.getLevelCode() != null
                && member.getLevelCode() < template.getMinLevel()) {
            result.setFailReason("会员等级不足");
            return result;
        }

        if (template.getDailyLimit() != null && template.getDailyLimit() > 0) {
            String dailyKey = RedisKeyUtil.dailyLimit(dto.getTemplateId(), dto.getMemberId());
            Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey, 1);
            if (dailyCount == 1) {
                long ttlSeconds = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
                stringRedisTemplate.expire(dailyKey, ttlSeconds, TimeUnit.SECONDS);
            }
            if (dailyCount != null && dailyCount > template.getDailyLimit()) {
                stringRedisTemplate.opsForValue().decrement(dailyKey);
                result.setFailReason("今日领券已达上限");
                return result;
            }
        }

        if (template.getTotalLimit() != null && template.getTotalLimit() > 0) {
            int totalCount = couponInstanceMapper.countMemberTotalReceive(dto.getMemberId(), dto.getTemplateId());
            if (totalCount >= template.getTotalLimit()) {
                if (template.getDailyLimit() != null && template.getDailyLimit() > 0) {
                    String dailyKey = RedisKeyUtil.dailyLimit(dto.getTemplateId(), dto.getMemberId());
                    stringRedisTemplate.opsForValue().decrement(dailyKey);
                }
                result.setFailReason("该券已达每人领取上限");
                return result;
            }
        }

        if (template.getTotalAmount() != null && template.getTotalAmount() != -1) {
            LambdaUpdateWrapper<CouponTemplate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CouponTemplate::getId, template.getId())
                    .lt(CouponTemplate::getReceivedCount, template.getTotalAmount())
                    .setSql("received_count = received_count + 1");
            int affected = couponTemplateMapper.update(null, updateWrapper);
            if (affected == 0) {
                if (template.getDailyLimit() != null && template.getDailyLimit() > 0) {
                    String dailyKey = RedisKeyUtil.dailyLimit(dto.getTemplateId(), dto.getMemberId());
                    stringRedisTemplate.opsForValue().decrement(dailyKey);
                }
                result.setFailReason("优惠券已领完");
                return result;
            }
        } else {
            LambdaUpdateWrapper<CouponTemplate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CouponTemplate::getId, template.getId())
                    .setSql("received_count = received_count + 1");
            couponTemplateMapper.update(null, updateWrapper);
        }

        CouponInstance instance = new CouponInstance();
        instance.setInstanceNo(generateInstanceNo());
        instance.setTemplateId(template.getId());
        instance.setMemberId(dto.getMemberId());
        instance.setReceiveSource(dto.getReceiveSource());
        instance.setReceiveTime(LocalDateTime.now());
        instance.setSourceId(dto.getSourceId());

        LocalDateTime validStart;
        LocalDateTime validEnd;
        if (template.getValidType() == 1) {
            validStart = template.getValidStart();
            validEnd = template.getValidEnd();
        } else {
            validStart = LocalDateTime.now();
            validEnd = validStart.plusDays(template.getValidDays());
        }
        instance.setValidStart(validStart);
        instance.setValidEnd(validEnd);

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validStart)) {
            instance.setCouponStatus(CouponStatusEnum.NOT_STARTED.getCode());
        } else if (now.isAfter(validEnd)) {
            instance.setCouponStatus(CouponStatusEnum.EXPIRED.getCode());
        } else {
            instance.setCouponStatus(CouponStatusEnum.AVAILABLE.getCode());
        }

        couponInstanceMapper.insert(instance);

        if (template.getActivityId() != null) {
            BigDecimal budgetAmount = template.getReduceAmount() != null ? template.getReduceAmount() : BigDecimal.ZERO;
            boolean budgetOk = budgetOccupyService.tryOccupyBudget(
                    template.getActivityId(), null, budgetAmount, 1, null, instance.getId());
            if (!budgetOk) {
                couponInstanceMapper.deleteById(instance.getId());
                if (template.getDailyLimit() != null && template.getDailyLimit() > 0) {
                    String dailyKey = RedisKeyUtil.dailyLimit(dto.getTemplateId(), dto.getMemberId());
                    stringRedisTemplate.opsForValue().decrement(dailyKey);
                }
                LambdaUpdateWrapper<CouponTemplate> rollbackWrapper = new LambdaUpdateWrapper<>();
                rollbackWrapper.eq(CouponTemplate::getId, template.getId())
                        .setSql("received_count = received_count - 1");
                couponTemplateMapper.update(null, rollbackWrapper);
                result.setFailReason("活动预算不足");
                return result;
            }
        }

        result.setSuccess(true);
        result.setInstanceId(instance.getId());
        result.setInstanceNo(instance.getInstanceNo());
        return result;
    }

    private String generateInstanceNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Random random = new Random();
        int randomPart = 10000 + random.nextInt(90000);
        return INSTANCE_NO_PREFIX + timePart + randomPart;
    }

    @Override
    public List<CouponReceiveResultVO> batchIssue(CouponBatchIssueDTO dto) {
        List<CouponReceiveResultVO> results = new ArrayList<>();
        for (Long memberId : dto.getMemberIds()) {
            CouponReceiveDTO receiveDTO = new CouponReceiveDTO();
            receiveDTO.setMemberId(memberId);
            receiveDTO.setTemplateId(dto.getTemplateId());
            receiveDTO.setReceiveSource(dto.getReceiveSource());
            receiveDTO.setSourceId(dto.getSourceId());
            CouponReceiveResultVO result;
            try {
                result = receiveCoupon(receiveDTO);
            } catch (Exception e) {
                result = new CouponReceiveResultVO();
                result.setSuccess(false);
                result.setFailReason(e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    @Override
    public CouponAvailabilityVO checkAvailability(CouponAvailabilityDTO dto) {
        CouponAvailabilityVO result = new CouponAvailabilityVO();
        result.setAvailable(false);

        CouponInstance instance = couponInstanceMapper.selectById(dto.getInstanceId());
        if (instance == null) {
            result.setUnavailReason("券不存在");
            return result;
        }

        CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            result.setUnavailReason("券模板不存在");
            return result;
        }

        if (!CouponStatusEnum.AVAILABLE.getCode().equals(instance.getCouponStatus())) {
            result.setUnavailReason("券状态不可用");
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        if (instance.getValidStart() != null && now.isBefore(instance.getValidStart())) {
            result.setUnavailReason("券尚未生效");
            return result;
        }
        if (instance.getValidEnd() != null && now.isAfter(instance.getValidEnd())) {
            result.setUnavailReason("券已过期");
            return result;
        }

        if (CouponTypeEnum.FULL_REDUCTION.getCode().equals(template.getCouponType())) {
            if (dto.getOrderAmount() == null) {
                result.setUnavailReason("缺少订单金额信息");
                return result;
            }
            if (dto.getOrderAmount().compareTo(template.getFullAmount()) < 0) {
                result.setUnavailReason("订单金额未达到满减门槛");
                return result;
            }
        }

        if (StringUtils.hasText(template.getExcludeItems()) && !CollectionUtils.isEmpty(dto.getItemIds())) {
            List<Long> excludeIdList = Arrays.stream(template.getExcludeItems().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            for (Long itemId : dto.getItemIds()) {
                if (excludeIdList.contains(itemId)) {
                    result.setUnavailReason("订单中包含不可用的商品");
                    return result;
                }
            }
        }

        result.setAvailable(true);
        if (CouponTypeEnum.FULL_REDUCTION.getCode().equals(template.getCouponType())) {
            result.setReduceAmount(template.getReduceAmount());
            result.setActualValue(template.getReduceAmount());
        } else {
            result.setActualValue(BigDecimal.ZERO);
        }
        return result;
    }

    @Override
    public IPage<CouponInstanceVO> pageMemberCoupons(CouponQueryDTO dto) {
        LambdaQueryWrapper<CouponInstance> wrapper = new LambdaQueryWrapper<>();
        if (dto.getMemberId() != null) {
            wrapper.eq(CouponInstance::getMemberId, dto.getMemberId());
        }
        if (dto.getTemplateId() != null) {
            wrapper.eq(CouponInstance::getTemplateId, dto.getTemplateId());
        }
        if (dto.getCouponStatus() != null) {
            wrapper.eq(CouponInstance::getCouponStatus, dto.getCouponStatus());
        }
        if (dto.getValidEndStart() != null) {
            wrapper.ge(CouponInstance::getValidEnd, dto.getValidEndStart());
        }
        if (dto.getValidEndEnd() != null) {
            wrapper.le(CouponInstance::getValidEnd, dto.getValidEndEnd());
        }

        Map<Integer, Integer> statusPriority = new HashMap<>();
        statusPriority.put(CouponStatusEnum.AVAILABLE.getCode(), 1);
        statusPriority.put(CouponStatusEnum.NOT_STARTED.getCode(), 2);
        statusPriority.put(CouponStatusEnum.LOCKED.getCode(), 3);
        statusPriority.put(CouponStatusEnum.USED.getCode(), 4);
        statusPriority.put(CouponStatusEnum.EXPIRED.getCode(), 5);

        wrapper.last("ORDER BY CASE coupon_status " +
                "WHEN " + CouponStatusEnum.AVAILABLE.getCode() + " THEN 1 " +
                "WHEN " + CouponStatusEnum.NOT_STARTED.getCode() + " THEN 2 " +
                "WHEN " + CouponStatusEnum.LOCKED.getCode() + " THEN 3 " +
                "WHEN " + CouponStatusEnum.USED.getCode() + " THEN 4 " +
                "WHEN " + CouponStatusEnum.EXPIRED.getCode() + " THEN 5 " +
                "ELSE 6 END, valid_end ASC");

        Page<CouponInstance> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<CouponInstance> instancePage = couponInstanceMapper.selectPage(page, wrapper);

        Page<CouponInstanceVO> voPage = new Page<>(instancePage.getCurrent(), instancePage.getSize(), instancePage.getTotal());
        if (!CollectionUtils.isEmpty(instancePage.getRecords())) {
            List<Long> templateIds = instancePage.getRecords().stream()
                    .map(CouponInstance::getTemplateId)
                    .distinct()
                    .collect(Collectors.toList());
            List<CouponTemplate> templates = couponTemplateMapper.selectBatchIds(templateIds);
            Map<Long, CouponTemplateVO> templateMap = templates.stream()
                    .collect(Collectors.toMap(CouponTemplate::getId, this::convertToTemplateVO));

            List<CouponInstanceVO> voList = instancePage.getRecords().stream()
                    .map(ins -> convertToInstanceVO(ins, templateMap.get(ins.getTemplateId())))
                    .collect(Collectors.toList());
            voPage.setRecords(voList);
        }
        return voPage;
    }

    @Override
    public CouponInstanceVO getInstance(Long instanceId) {
        CouponInstance instance = couponInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("券实例不存在");
        }
        CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
        CouponTemplateVO templateVO = template == null ? null : convertToTemplateVO(template);
        return convertToInstanceVO(instance, templateVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstanceStatus(Long instanceId, Integer targetStatus, Integer expectedStatus) {
        LambdaUpdateWrapper<CouponInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponInstance::getId, instanceId);
        if (expectedStatus != null) {
            wrapper.eq(CouponInstance::getCouponStatus, expectedStatus);
        }
        wrapper.set(CouponInstance::getCouponStatus, targetStatus);
        int affected = couponInstanceMapper.update(null, wrapper);
        if (affected == 0) {
            throw new BusinessException("更新券状态失败");
        }
    }

    @Override
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void expireCouponsTask() {
        log.info("开始执行优惠券过期定时任务");
        LocalDateTime now = LocalDateTime.now();

        int updated;
        do {
            LambdaUpdateWrapper<CouponInstance> expireWrapper = new LambdaUpdateWrapper<>();
            expireWrapper.in(CouponInstance::getCouponStatus,
                    CouponStatusEnum.NOT_STARTED.getCode(),
                    CouponStatusEnum.AVAILABLE.getCode(),
                    CouponStatusEnum.LOCKED.getCode());
            expireWrapper.lt(CouponInstance::getValidEnd, now);
            expireWrapper.set(CouponInstance::getCouponStatus, CouponStatusEnum.EXPIRED.getCode());
            expireWrapper.last("LIMIT " + BATCH_SIZE);
            updated = couponInstanceMapper.update(null, expireWrapper);
            log.info("批量更新过期券数量: {}", updated);
        } while (updated == BATCH_SIZE);

        do {
            LambdaUpdateWrapper<CouponInstance> startWrapper = new LambdaUpdateWrapper<>();
            startWrapper.eq(CouponInstance::getCouponStatus, CouponStatusEnum.NOT_STARTED.getCode());
            startWrapper.le(CouponInstance::getValidStart, now);
            startWrapper.gt(CouponInstance::getValidEnd, now);
            startWrapper.set(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode());
            startWrapper.last("LIMIT " + BATCH_SIZE);
            updated = couponInstanceMapper.update(null, startWrapper);
            log.info("批量更新未开始->可用券数量: {}", updated);
        } while (updated == BATCH_SIZE);

        do {
            LambdaUpdateWrapper<CouponInstance> rollbackWrapper = new LambdaUpdateWrapper<>();
            rollbackWrapper.eq(CouponInstance::getCouponStatus, CouponStatusEnum.AVAILABLE.getCode());
            rollbackWrapper.gt(CouponInstance::getValidStart, now);
            rollbackWrapper.set(CouponInstance::getCouponStatus, CouponStatusEnum.NOT_STARTED.getCode());
            rollbackWrapper.last("LIMIT " + BATCH_SIZE);
            updated = couponInstanceMapper.update(null, rollbackWrapper);
            log.info("批量更新可用->未开始券数量: {}", updated);
        } while (updated == BATCH_SIZE);

        log.info("优惠券过期定时任务执行完成");
    }

    @Override
    @Scheduled(cron = "0 0 10 * * ?")
    public void pushExpireReminderTask() {
        log.info("开始执行优惠券到期提醒定时任务");
        List<Integer> statusList = Arrays.asList(
                CouponStatusEnum.NOT_STARTED.getCode(),
                CouponStatusEnum.AVAILABLE.getCode(),
                CouponStatusEnum.LOCKED.getCode()
        );
        List<CouponInstance> expiringList = couponInstanceMapper.selectExpiringInDays(3, statusList);
        if (CollectionUtils.isEmpty(expiringList)) {
            log.info("没有即将过期的优惠券");
            return;
        }
        log.info("查询到{}张即将过期的优惠券", expiringList.size());
        for (CouponInstance instance : expiringList) {
            try {
                log.info("TODO: 发送到期提醒 - instanceId:{}, memberId:{}, validEnd:{}",
                        instance.getId(), instance.getMemberId(), instance.getValidEnd());
            } catch (Exception e) {
                log.error("发送到期提醒失败 - instanceId:{}", instance.getId(), e);
            }
        }
        log.info("优惠券到期提醒定时任务执行完成");
    }

    private CouponTemplateVO convertToTemplateVO(CouponTemplate template) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(template.getId());
        vo.setCouponCode(template.getCouponCode());
        vo.setCouponName(template.getCouponName());
        vo.setCouponType(template.getCouponType());
        vo.setTotalAmount(template.getTotalAmount());
        vo.setReceivedCount(template.getReceivedCount());
        vo.setUsedCount(template.getUsedCount());
        vo.setFullAmount(template.getFullAmount());
        vo.setReduceAmount(template.getReduceAmount());
        vo.setExchangeItem(template.getExchangeItem());
        vo.setValidType(template.getValidType());
        vo.setValidStart(template.getValidStart());
        vo.setValidEnd(template.getValidEnd());
        vo.setValidDays(template.getValidDays());
        vo.setMinLevel(template.getMinLevel());
        vo.setDailyLimit(template.getDailyLimit());
        vo.setTotalLimit(template.getTotalLimit());
        vo.setApplyScenes(template.getApplyScenes());
        vo.setExcludeItems(template.getExcludeItems());
        vo.setStackable(template.getStackable());
        vo.setDescription(template.getDescription());
        vo.setStatus(template.getStatus());
        vo.setActivityId(template.getActivityId());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());

        for (CouponTypeEnum typeEnum : CouponTypeEnum.values()) {
            if (typeEnum.getCode().equals(template.getCouponType())) {
                vo.setCouponTypeName(typeEnum.getName());
                break;
            }
        }
        vo.setStatusName(template.getStatus() != null && template.getStatus() == 1 ? "上架" : "下架");

        if (template.getTotalAmount() != null && template.getTotalAmount() == -1) {
            vo.setRemainCount(-1);
        } else {
            int total = template.getTotalAmount() == null ? 0 : template.getTotalAmount();
            int received = template.getReceivedCount() == null ? 0 : template.getReceivedCount();
            vo.setRemainCount(Math.max(0, total - received));
        }
        return vo;
    }

    private CouponInstanceVO convertToInstanceVO(CouponInstance instance, CouponTemplateVO templateVO) {
        CouponInstanceVO vo = new CouponInstanceVO();
        vo.setId(instance.getId());
        vo.setInstanceNo(instance.getInstanceNo());
        vo.setTemplateId(instance.getTemplateId());
        vo.setMemberId(instance.getMemberId());
        vo.setCouponStatus(instance.getCouponStatus());
        vo.setValidStart(instance.getValidStart());
        vo.setValidEnd(instance.getValidEnd());
        vo.setUsedTime(instance.getUsedTime());
        vo.setUsedOrderNo(instance.getUsedOrderNo());
        vo.setLockedTime(instance.getLockedTime());
        vo.setLockOrderNo(instance.getLockOrderNo());
        vo.setReceiveSource(instance.getReceiveSource());
        vo.setReceiveTime(instance.getReceiveTime());
        vo.setSourceId(instance.getSourceId());
        vo.setRemark(instance.getRemark());
        vo.setCreateTime(instance.getCreateTime());
        vo.setUpdateTime(instance.getUpdateTime());
        vo.setTemplate(templateVO);

        LocalDateTime now = LocalDateTime.now();
        if (instance.getValidEnd() != null) {
            if (CouponStatusEnum.EXPIRED.getCode().equals(instance.getCouponStatus())) {
                vo.setProgressBarText("已过期");
            } else {
                long daysLeft = Duration.between(now, instance.getValidEnd()).toDays();
                if (daysLeft <= 3) {
                    vo.setProgressBarText("过期前" + (daysLeft + 1) + "天");
                } else if (daysLeft <= 7) {
                    vo.setProgressBarText("还有" + daysLeft + "天过期");
                } else {
                    vo.setProgressBarText("有效期内");
                }
            }
        }
        return vo;
    }
}
