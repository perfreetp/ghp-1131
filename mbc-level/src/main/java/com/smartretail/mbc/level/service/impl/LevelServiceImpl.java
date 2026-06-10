package com.smartretail.mbc.level.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.level.dto.BirthdayBenefitDTO;
import com.smartretail.mbc.level.dto.GrowthCalcDTO;
import com.smartretail.mbc.level.dto.LevelAdjustDTO;
import com.smartretail.mbc.level.dto.LevelRuleUpsertDTO;
import com.smartretail.mbc.level.entity.GrowthLog;
import com.smartretail.mbc.level.entity.LevelRule;
import com.smartretail.mbc.level.mapper.GrowthLogMapper;
import com.smartretail.mbc.level.mapper.LevelRuleMapper;
import com.smartretail.mbc.level.service.LevelService;
import com.smartretail.mbc.level.vo.BirthdayBenefitResultVO;
import com.smartretail.mbc.level.vo.GrowthResultVO;
import com.smartretail.mbc.level.vo.LevelRuleVO;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelServiceImpl implements LevelService {

    private final LevelRuleMapper levelRuleMapper;
    private final GrowthLogMapper growthLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberMapper memberMapper;

    private static final String BIRTHDAY_KEY_PREFIX = "mbc:birthday:";

    @Override
    public List<LevelRuleVO> listAllRules() {
        List<LevelRule> rules = levelRuleMapper.selectList(
                new LambdaQueryWrapper<LevelRule>().orderByAsc(LevelRule::getLevelCode)
        );
        List<LevelRuleVO> result = new ArrayList<>();
        for (LevelRule rule : rules) {
            LevelRuleVO vo = convertToVO(rule);
            Integer count = memberMapper.selectCount(
                    new LambdaQueryWrapper<Member>().eq(Member::getLevelCode, rule.getLevelCode())
            ).intValue();
            vo.setMemberCount(count);
            result.add(vo);
        }
        return result;
    }

    @Override
    public LevelRuleVO getRuleByCode(Integer levelCode) {
        if (levelCode == null) {
            throw new BusinessException("等级编码不能为空");
        }
        LevelRule rule = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>().eq(LevelRule::getLevelCode, levelCode)
        );
        if (rule == null) {
            return null;
        }
        LevelRuleVO vo = convertToVO(rule);
        Integer count = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>().eq(Member::getLevelCode, rule.getLevelCode())
        ).intValue();
        vo.setMemberCount(count);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertRule(LevelRuleUpsertDTO dto) {
        if (dto.getLevelCode() == null) {
            throw new BusinessException("等级编码不能为空");
        }
        LevelRule existing = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>().eq(LevelRule::getLevelCode, dto.getLevelCode())
        );
        LevelRule rule = new LevelRule();
        BeanUtils.copyProperties(dto, rule);
        if (existing != null) {
            rule.setId(existing.getId());
            levelRuleMapper.updateById(rule);
        } else {
            levelRuleMapper.insert(rule);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthResultVO calcAndAddGrowth(GrowthCalcDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        LevelRule rule = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>().eq(LevelRule::getLevelCode, member.getLevelCode())
        );
        BigDecimal growthRatio = rule != null && rule.getGrowthRatio() != null
                ? rule.getGrowthRatio() : BigDecimal.ONE;

        BigDecimal growthDecimal = dto.getOrderAmount().multiply(growthRatio);
        int growthToAdd = growthDecimal.setScale(0, RoundingMode.HALF_UP).intValue();

        return doAddGrowth(member, growthToAdd, dto.getSourceType(), dto.getSourceId(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthResultVO adjustLevel(LevelAdjustDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        String remark = dto.getReason() != null ? dto.getReason() : "人工调整";
        return doAddGrowth(member, dto.getAdjustGrowth(), 99, null, remark);
    }

    @Override
    public LevelRuleVO getCurrentLevel(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return getRuleByCode(member.getLevelCode());
    }

    @Override
    public boolean isLevelUp(Integer beforeCode, Integer afterCode) {
        if (beforeCode == null || afterCode == null) {
            return false;
        }
        return afterCode > beforeCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BirthdayBenefitResultVO grantBirthdayBenefit(BirthdayBenefitDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        int year = LocalDate.now().getYear();
        String lockKey = BIRTHDAY_KEY_PREFIX + year + ":" + dto.getMemberId();
        Boolean setSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 365, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(setSuccess)) {
            BirthdayBenefitResultVO result = new BirthdayBenefitResultVO();
            result.setMemberId(dto.getMemberId());
            result.setGrantedPoints(0);
            result.setGrantedCouponIds(Collections.emptyList());
            result.setMessage("本年度生日权益已发放，请勿重复领取");
            return result;
        }

        LevelRule rule = levelRuleMapper.selectOne(
                new LambdaQueryWrapper<LevelRule>().eq(LevelRule::getLevelCode, member.getLevelCode())
        );

        BirthdayBenefitResultVO result = new BirthdayBenefitResultVO();
        result.setMemberId(dto.getMemberId());

        Integer grantedPoints = 0;
        List<Long> grantedCouponIds = new ArrayList<>();

        if (rule != null) {
            if (rule.getBirthdayPoints() != null && rule.getBirthdayPoints() > 0) {
                grantedPoints = rule.getBirthdayPoints();
                // TODO: 调用积分模块 PointFacade.addPoints(memberId, grantedPoints, "生日赠送")
                log.info("TODO: 发放生日积分 memberId={}, points={}", dto.getMemberId(), grantedPoints);
            }

            List<Long> couponIds = dto.getTemplateIds() != null && !dto.getTemplateIds().isEmpty()
                    ? dto.getTemplateIds()
                    : (rule.getBirthdayCouponId() != null ? List.of(rule.getBirthdayCouponId()) : Collections.emptyList());

            for (Long couponId : couponIds) {
                // TODO: 调用优惠券模块 CouponFacade.directIssueCoupon(memberId, couponId)
                log.info("TODO: 发放生日优惠券 memberId={}, couponId={}", dto.getMemberId(), couponId);
                grantedCouponIds.add(couponId);
            }
        }

        result.setGrantedPoints(grantedPoints);
        result.setGrantedCouponIds(grantedCouponIds);
        result.setMessage("生日权益发放成功，积分：" + grantedPoints + "，优惠券：" + grantedCouponIds.size() + "张");
        return result;
    }

    @Override
    @Async
    public void processLevelChangeAsync(Long memberId, Integer beforeLevel, Integer afterLevel) {
        log.info("异步处理等级变更 memberId={}, beforeLevel={}, afterLevel={}", memberId, beforeLevel, afterLevel);
        // TODO: 发送等级变更消息 MessageFacade.sendLevelChangeMessage(memberId, beforeLevel, afterLevel)
        // TODO: 刷新会员缓存 memberService.evictMemberCache(...)
    }

    private GrowthResultVO doAddGrowth(Member member, int growthToAdd, Integer sourceType, String sourceId, String remark) {
        Integer beforeGrowth = member.getGrowthValue() != null ? member.getGrowthValue() : 0;
        int afterGrowth = Math.max(0, beforeGrowth + growthToAdd);

        Integer beforeLevel = member.getLevelCode();
        Integer afterLevel = calculateLevel(afterGrowth);

        memberMapper.update(null,
                new LambdaUpdateWrapper<Member>()
                        .eq(Member::getId, member.getId())
                        .set(Member::getGrowthValue, afterGrowth)
                        .set(Member::getLevelCode, afterLevel)
        );

        GrowthLog growthLog = new GrowthLog();
        growthLog.setMemberId(member.getId());
        growthLog.setChangeValue(growthToAdd);
        growthLog.setBeforeValue(beforeGrowth);
        growthLog.setAfterValue(afterGrowth);
        growthLog.setBeforeLevel(beforeLevel);
        growthLog.setAfterLevel(afterLevel);
        growthLog.setSourceType(sourceType);
        growthLog.setSourceId(sourceId);
        growthLog.setRemark(remark);
        growthLog.setCreateTime(LocalDateTime.now());
        growthLog.setCreateBy("system");
        growthLogMapper.insert(growthLog);

        boolean levelUp = isLevelUp(beforeLevel, afterLevel);
        if (levelUp) {
            processLevelChangeAsync(member.getId(), beforeLevel, afterLevel);
        }

        GrowthResultVO result = new GrowthResultVO();
        result.setMemberId(member.getId());
        result.setBeforeLevel(beforeLevel);
        result.setAfterLevel(afterLevel);
        result.setLevelUp(levelUp);
        result.setGrowthChange(growthToAdd);
        result.setCurrentGrowth(afterGrowth);
        return result;
    }

    private Integer calculateLevel(Integer growth) {
        List<LevelRule> rules = levelRuleMapper.selectList(
                new LambdaQueryWrapper<LevelRule>()
                        .eq(LevelRule::getStatus, 1)
                        .orderByDesc(LevelRule::getGrowthThreshold)
        );
        if (rules == null || rules.isEmpty()) {
            return MemberLevelEnum.getLevelByGrowth(growth).getCode();
        }
        for (LevelRule rule : rules) {
            if (growth >= (rule.getGrowthThreshold() != null ? rule.getGrowthThreshold() : 0)) {
                return rule.getLevelCode();
            }
        }
        return rules.get(rules.size() - 1).getLevelCode();
    }

    private LevelRuleVO convertToVO(LevelRule rule) {
        LevelRuleVO vo = new LevelRuleVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }
}
