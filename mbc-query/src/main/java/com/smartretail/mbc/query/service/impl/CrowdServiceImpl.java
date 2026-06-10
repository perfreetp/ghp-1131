package com.smartretail.mbc.query.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.enums.CrowdRuleTypeEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.query.dto.CrowdCalcDTO;
import com.smartretail.mbc.query.dto.CrowdGroupCreateDTO;
import com.smartretail.mbc.query.dto.CrowdGroupUpdateDTO;
import com.smartretail.mbc.query.dto.CrowdMemberQueryDTO;
import com.smartretail.mbc.query.entity.CrowdGroup;
import com.smartretail.mbc.query.entity.CrowdMember;
import com.smartretail.mbc.query.mapper.CrowdGroupMapper;
import com.smartretail.mbc.query.mapper.CrowdMemberMapper;
import com.smartretail.mbc.query.service.CrowdService;
import com.smartretail.mbc.query.vo.CrowdGroupVO;
import com.smartretail.mbc.query.vo.CrowdMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrowdServiceImpl implements CrowdService {

    private final CrowdGroupMapper crowdGroupMapper;
    private final CrowdMemberMapper crowdMemberMapper;
    private final MemberMapper memberMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final int BATCH_SIZE = 500;
    private static final String CROWD_CALC_LOCK_KEY = "crowd:calc:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 300;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCrowd(CrowdGroupCreateDTO dto) {
        Long existCount = crowdGroupMapper.selectCount(
                new LambdaQueryWrapper<CrowdGroup>().eq(CrowdGroup::getCrowdCode, dto.getCrowdCode())
        );
        if (existCount != null && existCount > 0) {
            throw new BusinessException("人群编码已存在");
        }

        CrowdGroup crowdGroup = new CrowdGroup();
        BeanUtils.copyProperties(dto, crowdGroup);
        crowdGroup.setRuleConfig(buildRuleConfig(dto.getRules()));
        crowdGroup.setStatus(0);
        crowdGroup.setActualCount(0);
        crowdGroup.setEstimatedCount(0);
        crowdGroupMapper.insert(crowdGroup);
        return crowdGroup.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCrowd(CrowdGroupUpdateDTO dto) {
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(dto.getCrowdId());
        if (crowdGroup == null) {
            throw new BusinessException("人群不存在");
        }

        CrowdGroup update = new CrowdGroup();
        update.setId(dto.getCrowdId());
        if (StringUtils.hasText(dto.getCrowdName())) {
            update.setCrowdName(dto.getCrowdName());
        }
        if (dto.getRules() != null) {
            update.setRuleConfig(buildRuleConfig(dto.getRules()));
        }
        if (dto.getDescription() != null) {
            update.setDescription(dto.getDescription());
        }
        crowdGroupMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCrowd(Long crowdId) {
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(crowdId);
        if (crowdGroup == null) {
            throw new BusinessException("人群不存在");
        }
        crowdGroupMapper.deleteById(crowdId);
        crowdMemberMapper.delete(
                new LambdaQueryWrapper<CrowdMember>().eq(CrowdMember::getCrowdId, crowdId)
        );
    }

    @Override
    public CrowdGroupVO getCrowdDetail(Long crowdId) {
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(crowdId);
        if (crowdGroup == null) {
            throw new BusinessException("人群不存在");
        }
        return convertToCrowdGroupVO(crowdGroup);
    }

    @Override
    public IPage<CrowdGroupVO> pageCrowds(String keyword, Integer crowdType, Integer status, Integer pageNum, Integer pageSize) {
        Page<CrowdGroup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CrowdGroup> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrowdGroup::getCrowdCode, keyword)
                    .or().like(CrowdGroup::getCrowdName, keyword));
        }
        if (crowdType != null) {
            wrapper.eq(CrowdGroup::getCrowdType, crowdType);
        }
        if (status != null) {
            wrapper.eq(CrowdGroup::getStatus, status);
        }
        wrapper.orderByDesc(CrowdGroup::getCreateTime);

        IPage<CrowdGroup> groupPage = crowdGroupMapper.selectPage(page, wrapper);
        IPage<CrowdGroupVO> resultPage = new Page<>(groupPage.getCurrent(), groupPage.getSize(), groupPage.getTotal());
        List<CrowdGroupVO> voList = groupPage.getRecords().stream()
                .map(this::convertToCrowdGroupVO)
                .collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer calcCrowd(CrowdCalcDTO dto) {
        String lockKey = CROWD_CALC_LOCK_KEY + dto.getCrowdId();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("人群计算中，请稍后再试");
        }

        try {
            CrowdGroup crowdGroup = crowdGroupMapper.selectById(dto.getCrowdId());
            if (crowdGroup == null) {
                throw new BusinessException("人群不存在");
            }

            List<CrowdGroupCreateDTO.CrowdRuleItem> rules = parseRuleConfig(crowdGroup.getRuleConfig());
            if (CollectionUtils.isEmpty(rules)) {
                throw new BusinessException("人群规则为空，无法计算");
            }

            Set<Long> matchedMemberIds = calculateMatchedMembers(rules);

            crowdMemberMapper.delete(
                    new LambdaQueryWrapper<CrowdMember>().eq(CrowdMember::getCrowdId, dto.getCrowdId())
            );

            if (!CollectionUtils.isEmpty(matchedMemberIds)) {
                batchInsertCrowdMembers(dto.getCrowdId(), new ArrayList<>(matchedMemberIds), rules);
            }

            CrowdGroup update = new CrowdGroup();
            update.setId(dto.getCrowdId());
            update.setActualCount(matchedMemberIds.size());
            update.setRefreshTime(LocalDateTime.now());
            if (crowdGroup.getStatus() != null && crowdGroup.getStatus() == 0) {
                update.setStatus(1);
            }
            crowdGroupMapper.updateById(update);

            return matchedMemberIds.size();
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public IPage<CrowdMemberVO> pageCrowdMembers(CrowdMemberQueryDTO dto) {
        CrowdGroup crowdGroup = crowdGroupMapper.selectById(dto.getCrowdId());
        if (crowdGroup == null) {
            throw new BusinessException("人群不存在");
        }

        Page<CrowdMember> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<CrowdMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrowdMember::getCrowdId, dto.getCrowdId())
                .eq(CrowdMember::getIsActive, 1);
        wrapper.orderByDesc(CrowdMember::getMatchTime);

        IPage<CrowdMember> memberPage = crowdMemberMapper.selectPage(page, wrapper);
        IPage<CrowdMemberVO> resultPage = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());

        if (CollectionUtils.isEmpty(memberPage.getRecords())) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        List<Long> memberIds = memberPage.getRecords().stream()
                .map(CrowdMember::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, Member> memberMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(memberIds)) {
            List<Member> members = memberMapper.selectBatchIds(memberIds);
            for (Member m : members) {
                memberMap.put(m.getId(), m);
            }
        }

        Map<Long, CrowdMember> crowdMemberMap = memberPage.getRecords().stream()
                .collect(Collectors.toMap(CrowdMember::getMemberId, cm -> cm, (a, b) -> a));

        List<CrowdMemberVO> voList = new ArrayList<>();
        for (Long memberId : memberIds) {
            Member member = memberMap.get(memberId);
            CrowdMember crowdMember = crowdMemberMap.get(memberId);
            if (member != null && crowdMember != null) {
                CrowdMemberVO vo = new CrowdMemberVO();
                vo.setMemberId(member.getId());
                vo.setMemberCode(member.getMemberCode());
                vo.setPhone(member.getPhone());
                vo.setName(member.getName());
                vo.setLevelCode(member.getLevelCode());
                MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
                vo.setLevelName(levelEnum.getName());
                vo.setCurrentPoints(member.getCurrentPoints());
                vo.setMatchTime(crowdMember.getMatchTime());
                vo.setMatchReason(crowdMember.getMatchReason());
                voList.add(vo);
            }
        }

        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public boolean isMemberInCrowd(Long crowdId, Long memberId) {
        if (crowdId == null || memberId == null) {
            return false;
        }
        Long count = crowdMemberMapper.selectCount(
                new LambdaQueryWrapper<CrowdMember>()
                        .eq(CrowdMember::getCrowdId, crowdId)
                        .eq(CrowdMember::getMemberId, memberId)
                        .eq(CrowdMember::getIsActive, 1)
        );
        return count != null && count > 0;
    }

    private String buildRuleConfig(List<CrowdGroupCreateDTO.CrowdRuleItem> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return "[]";
        }
        return JSON.toJSONString(rules);
    }

    private List<CrowdGroupCreateDTO.CrowdRuleItem> parseRuleConfig(String ruleConfig) {
        if (!StringUtils.hasText(ruleConfig)) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(ruleConfig, CrowdGroupCreateDTO.CrowdRuleItem.class);
        } catch (Exception e) {
            log.error("解析人群规则配置失败", e);
            return new ArrayList<>();
        }
    }

    private CrowdGroupVO convertToCrowdGroupVO(CrowdGroup crowdGroup) {
        CrowdGroupVO vo = new CrowdGroupVO();
        BeanUtils.copyProperties(crowdGroup, vo);
        vo.setRuleList(parseRuleConfig(crowdGroup.getRuleConfig()));
        vo.setCrowdTypeName(getCrowdTypeName(crowdGroup.getCrowdType()));
        vo.setStatusName(getStatusName(crowdGroup.getStatus()));
        vo.setPreviewMemberCount(crowdGroup.getActualCount() != null ? crowdGroup.getActualCount() : 0);
        return vo;
    }

    private String getCrowdTypeName(Integer crowdType) {
        if (crowdType == null) return "";
        switch (crowdType) {
            case 1: return "静态人群";
            case 2: return "动态人群";
            default: return "";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已生效";
            case 2: return "已失效";
            default: return "";
        }
    }

    private Set<Long> calculateMatchedMembers(List<CrowdGroupCreateDTO.CrowdRuleItem> rules) {
        Set<Long> resultSet = null;

        for (CrowdGroupCreateDTO.CrowdRuleItem rule : rules) {
            Set<Long> currentSet = applyRule(rule);
            if (resultSet == null) {
                resultSet = currentSet;
            } else {
                resultSet.retainAll(currentSet);
            }
            if (CollectionUtils.isEmpty(resultSet)) {
                break;
            }
        }

        return resultSet != null ? resultSet : new HashSet<>();
    }

    private Set<Long> applyRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        CrowdRuleTypeEnum ruleType = CrowdRuleTypeEnum.getByCode(rule.getRuleType());
        if (ruleType == null) {
            return new HashSet<>();
        }

        switch (ruleType) {
            case LEVEL:
                return applyLevelRule(rule);
            case CONSUME_AMOUNT:
                return applyConsumeAmountRule(rule);
            case CONSUME_COUNT:
                return applyConsumeCountRule(rule);
            case LAST_VISIT_DAYS:
                return applyLastVisitDaysRule(rule);
            case BIRTHDAY_MONTH:
                return applyBirthdayMonthRule(rule);
            case COUPON_PREFERENCE:
                return applyCouponPreferenceRule(rule);
            case POINT_RANGE:
                return applyPointRangeRule(rule);
            case TOTAL_SPENT:
                return applyTotalSpentRule(rule);
            default:
                return new HashSet<>();
        }
    }

    private Set<Long> applyLevelRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        List<Object> values = rule.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return new HashSet<>();
        }
        List<Integer> levelCodes = values.stream()
                .map(v -> Integer.parseInt(v.toString()))
                .collect(Collectors.toList());

        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .in(Member::getLevelCode, levelCodes)
                        .select(Member::getId)
        );
        return members.stream().map(Member::getId).collect(Collectors.toSet());
    }

    private Set<Long> applyConsumeAmountRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        List<ConsumeOrder> orders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .ge(ConsumeOrder::getPayTime, thirtyDaysAgo)
                        .le(ConsumeOrder::getPayTime, now)
                        .in(ConsumeOrder::getOrderStatus, java.util.Arrays.asList(1, 2))
                        .isNotNull(ConsumeOrder::getMemberId)
        );

        Map<Long, BigDecimal> memberAmountMap = new HashMap<>();
        for (ConsumeOrder order : orders) {
            Long memberId = order.getMemberId();
            BigDecimal amount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
            memberAmountMap.merge(memberId, amount, BigDecimal::add);
        }

        return filterByRange(memberAmountMap, rule);
    }

    private Set<Long> applyConsumeCountRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        List<ConsumeOrder> orders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .ge(ConsumeOrder::getPayTime, thirtyDaysAgo)
                        .le(ConsumeOrder::getPayTime, now)
                        .in(ConsumeOrder::getOrderStatus, java.util.Arrays.asList(1, 2))
                        .isNotNull(ConsumeOrder::getMemberId)
        );

        Map<Long, Long> memberCountMap = orders.stream()
                .collect(Collectors.groupingBy(ConsumeOrder::getMemberId, Collectors.counting()));

        Map<Long, BigDecimal> countDecimalMap = new HashMap<>();
        memberCountMap.forEach((k, v) -> countDecimalMap.put(k, new BigDecimal(v)));

        return filterByRange(countDecimalMap, rule);
    }

    private Set<Long> applyLastVisitDaysRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        Object valueMin = rule.getValueMin();
        if (valueMin == null) {
            return new HashSet<>();
        }
        int days = Integer.parseInt(valueMin.toString());
        LocalDateTime daysAgo = LocalDateTime.now().minus(days, ChronoUnit.DAYS);

        List<ConsumeOrder> recentOrders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .ge(ConsumeOrder::getPayTime, daysAgo)
                        .in(ConsumeOrder::getOrderStatus, java.util.Arrays.asList(1, 2))
                        .isNotNull(ConsumeOrder::getMemberId)
                        .select(ConsumeOrder::getMemberId)
        );
        Set<Long> recentMemberIds = recentOrders.stream()
                .map(ConsumeOrder::getMemberId)
                .collect(Collectors.toSet());

        List<Member> allMembers = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().select(Member::getId)
        );

        return allMembers.stream()
                .map(Member::getId)
                .filter(id -> !recentMemberIds.contains(id))
                .collect(Collectors.toSet());
    }

    private Set<Long> applyBirthdayMonthRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        List<Object> values = rule.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return new HashSet<>();
        }
        List<Integer> months = values.stream()
                .map(v -> Integer.parseInt(v.toString()))
                .collect(Collectors.toList());

        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .isNotNull(Member::getBirthday)
                        .select(Member::getId, Member::getBirthday)
        );

        Set<Long> result = new HashSet<>();
        for (Member member : members) {
            if (member.getBirthday() != null) {
                int month = member.getBirthday().getMonthValue();
                if (months.contains(month)) {
                    result.add(member.getId());
                }
            }
        }
        return result;
    }

    private Set<Long> applyCouponPreferenceRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        List<Object> values = rule.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return new HashSet<>();
        }
        Integer targetCouponType = Integer.parseInt(values.get(0).toString());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime ninetyDaysAgo = now.minus(90, ChronoUnit.DAYS);

        List<CouponInstance> usedCoupons = couponInstanceMapper.selectList(
                new LambdaQueryWrapper<CouponInstance>()
                        .eq(CouponInstance::getCouponStatus, 2)
                        .ge(CouponInstance::getUsedTime, ninetyDaysAgo)
                        .le(CouponInstance::getUsedTime, now)
                        .isNotNull(CouponInstance::getMemberId)
                        .isNotNull(CouponInstance::getTemplateId)
        );

        if (CollectionUtils.isEmpty(usedCoupons)) {
            return new HashSet<>();
        }

        List<Long> templateIds = usedCoupons.stream()
                .map(CouponInstance::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Integer> templateTypeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(templateIds)) {
            List<CouponTemplate> templates = couponTemplateMapper.selectBatchIds(templateIds);
            for (CouponTemplate t : templates) {
                templateTypeMap.put(t.getId(), t.getCouponType());
            }
        }

        Map<Long, Map<Integer, Integer>> memberTypeCountMap = new HashMap<>();
        for (CouponInstance coupon : usedCoupons) {
            Long memberId = coupon.getMemberId();
            Integer templateId = coupon.getTemplateId();
            Integer couponType = templateTypeMap.get(templateId);
            if (couponType == null) continue;

            memberTypeCountMap.computeIfAbsent(memberId, k -> new HashMap<>())
                    .merge(couponType, 1, Integer::sum);
        }

        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, Map<Integer, Integer>> entry : memberTypeCountMap.entrySet()) {
            Map<Integer, Integer> typeCount = entry.getValue();
            int maxCount = 0;
            Integer maxType = null;
            for (Map.Entry<Integer, Integer> typeEntry : typeCount.entrySet()) {
                if (typeEntry.getValue() > maxCount) {
                    maxCount = typeEntry.getValue();
                    maxType = typeEntry.getKey();
                }
            }
            if (maxType != null && maxType.equals(targetCouponType)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private Set<Long> applyPointRangeRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        BigDecimal min = rule.getValueMin() != null ? new BigDecimal(rule.getValueMin().toString()) : null;
        BigDecimal max = rule.getValueMax() != null ? new BigDecimal(rule.getValueMax().toString()) : null;

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (min != null) {
            wrapper.ge(Member::getCurrentPoints, min.intValue());
        }
        if (max != null) {
            wrapper.le(Member::getCurrentPoints, max.intValue());
        }
        wrapper.select(Member::getId);

        List<Member> members = memberMapper.selectList(wrapper);
        return members.stream().map(Member::getId).collect(Collectors.toSet());
    }

    private Set<Long> applyTotalSpentRule(CrowdGroupCreateDTO.CrowdRuleItem rule) {
        List<ConsumeOrder> orders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .in(ConsumeOrder::getOrderStatus, java.util.Arrays.asList(1, 2))
                        .isNotNull(ConsumeOrder::getMemberId)
        );

        Map<Long, BigDecimal> memberAmountMap = new HashMap<>();
        for (ConsumeOrder order : orders) {
            Long memberId = order.getMemberId();
            BigDecimal amount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
            memberAmountMap.merge(memberId, amount, BigDecimal::add);
        }

        return filterByRange(memberAmountMap, rule);
    }

    private Set<Long> filterByRange(Map<Long, BigDecimal> memberValueMap, CrowdGroupCreateDTO.CrowdRuleItem rule) {
        String operator = rule.getOperator();
        BigDecimal min = rule.getValueMin() != null ? new BigDecimal(rule.getValueMin().toString()) : null;
        BigDecimal max = rule.getValueMax() != null ? new BigDecimal(rule.getValueMax().toString()) : null;
        List<Object> values = rule.getValues();

        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, BigDecimal> entry : memberValueMap.entrySet()) {
            BigDecimal value = entry.getValue();
            boolean match = false;

            switch (operator) {
                case ">":
                    match = min != null && value.compareTo(min) > 0;
                    break;
                case ">=":
                    match = min != null && value.compareTo(min) >= 0;
                    break;
                case "<":
                    match = max != null && value.compareTo(max) < 0;
                    break;
                case "<=":
                    match = max != null && value.compareTo(max) <= 0;
                    break;
                case "=":
                    match = min != null && value.compareTo(min) == 0;
                    break;
                case "between":
                    match = min != null && max != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
                    break;
                case "in":
                    if (!CollectionUtils.isEmpty(values)) {
                        for (Object v : values) {
                            if (value.compareTo(new BigDecimal(v.toString())) == 0) {
                                match = true;
                                break;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            if (match) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private void batchInsertCrowdMembers(Long crowdId, List<Long> memberIds, List<CrowdGroupCreateDTO.CrowdRuleItem> rules) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return;
        }

        String matchReason = buildMatchReason(rules);
        LocalDateTime now = LocalDateTime.now();

        List<CrowdMember> batch = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            CrowdMember cm = new CrowdMember();
            cm.setCrowdId(crowdId);
            cm.setMemberId(memberIds.get(i));
            cm.setMatchTime(now);
            cm.setIsActive(1);
            cm.setMatchReason(matchReason);
            cm.setIsDeleted(0);
            batch.add(cm);

            if (batch.size() >= BATCH_SIZE || i == memberIds.size() - 1) {
                crowdMemberMapper.batchInsert(batch);
                batch.clear();
            }
        }
    }

    private String buildMatchReason(List<CrowdGroupCreateDTO.CrowdRuleItem> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return "";
        }
        List<String> reasons = new ArrayList<>();
        for (CrowdGroupCreateDTO.CrowdRuleItem rule : rules) {
            CrowdRuleTypeEnum ruleType = CrowdRuleTypeEnum.getByCode(rule.getRuleType());
            if (ruleType != null) {
                reasons.add(ruleType.getName());
            }
        }
        return String.join("、", reasons);
    }
}
