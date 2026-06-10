package com.smartretail.mbc.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.enums.OrderStatusEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.util.MemberCodeUtil;
import com.smartretail.mbc.common.util.RedisKeyUtil;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.member.dto.MemberIdentityDTO;
import com.smartretail.mbc.member.dto.MemberMergeDTO;
import com.smartretail.mbc.member.dto.MemberMergeLogQueryDTO;
import com.smartretail.mbc.member.dto.MemberMergePreviewDTO;
import com.smartretail.mbc.member.dto.MemberQueryDTO;
import com.smartretail.mbc.member.dto.MemberRegisterDTO;
import com.smartretail.mbc.member.dto.MemberUpdateDTO;
import com.smartretail.mbc.member.entity.Member;
import com.smartretail.mbc.member.entity.MemberMergeLog;
import com.smartretail.mbc.member.mapper.MemberMapper;
import com.smartretail.mbc.member.mapper.MemberMergeLogMapper;
import com.smartretail.mbc.member.service.MemberService;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import com.smartretail.mbc.member.vo.MemberMergePreviewVO;
import com.smartretail.mbc.member.vo.MemberVO;
import com.smartretail.mbc.member.vo.MergeLogVO;
import com.smartretail.mbc.member.vo.MergeResultVO;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberMergeLogMapper memberMergeLogMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MEMBER_ID_PREFIX = "mbc:member:id:";
    private static final String MERGE_LOCK_PREFIX = "mbc:lock:merge:";
    private static final long CACHE_TTL_HOURS = 1;
    private static final long LOCK_TIMEOUT_SECONDS = 30;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberVO register(MemberRegisterDTO dto) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getPhone, dto.getPhone());
        wrapper.isNull(Member::getMergedTo);
        Member existMember = memberMapper.selectOne(wrapper);
        if (existMember != null) {
            throw new BusinessException("该手机号已注册");
        }

        String memberCode;
        int retryCount = 0;
        do {
            memberCode = MemberCodeUtil.generateMemberCode();
            LambdaQueryWrapper<Member> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(Member::getMemberCode, memberCode);
            Member codeExist = memberMapper.selectOne(codeWrapper);
            if (codeExist == null) {
                break;
            }
            retryCount++;
            if (retryCount >= 10) {
                throw new BusinessException("生成会员码失败，请重试");
            }
        } while (true);

        Member member = new Member();
        member.setMemberCode(memberCode);
        member.setPhone(dto.getPhone());
        member.setName(dto.getName());
        member.setNickname(dto.getNickname());
        member.setGender(dto.getGender());
        member.setBirthday(dto.getBirthday());
        member.setLevelCode(1);
        member.setGrowthValue(0);
        member.setCurrentPoints(0);
        member.setTotalPoints(0);
        member.setRegisterSource(dto.getRegisterSource());
        member.setStatus(1);
        memberMapper.insert(member);

        // TODO: 调用 PointFacade 赠送10注册积分
        // pointFacade.addPoints(member.getId(), 10, PointSourceEnum.REGISTER);

        writeMemberToCache(member);

        return convertToVO(member);
    }

    @Override
    public MemberVO getById(Long memberId) {
        if (memberId == null) {
            return null;
        }
        String key = MEMBER_ID_PREFIX + memberId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(json)) {
            try {
                Member member = objectMapper.readValue(json, Member.class);
                return convertToVO(member);
            } catch (JsonProcessingException e) {
                log.warn("解析会员缓存失败，memberId={}", memberId, e);
            }
        }

        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return null;
        }
        writeMemberToCache(member);
        return convertToVO(member);
    }

    @Override
    public MemberVO getByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String phoneKey = RedisKeyUtil.phone(phone);
        String memberIdStr = stringRedisTemplate.opsForValue().get(phoneKey);
        if (StringUtils.hasText(memberIdStr)) {
            MemberVO vo = getById(Long.parseLong(memberIdStr));
            if (vo != null) {
                return vo;
            }
        }

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getPhone, phone);
        wrapper.isNull(Member::getMergedTo);
        Member member = memberMapper.selectOne(wrapper);
        if (member == null) {
            return null;
        }
        writeMemberToCache(member);
        return convertToVO(member);
    }

    @Override
    public MemberVO getByMemberCode(String memberCode) {
        if (!StringUtils.hasText(memberCode)) {
            return null;
        }
        String codeKey = RedisKeyUtil.memberCode(memberCode);
        String memberIdStr = stringRedisTemplate.opsForValue().get(codeKey);
        if (StringUtils.hasText(memberIdStr)) {
            MemberVO vo = getById(Long.parseLong(memberIdStr));
            if (vo != null) {
                return vo;
            }
        }

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getMemberCode, memberCode);
        wrapper.isNull(Member::getMergedTo);
        Member member = memberMapper.selectOne(wrapper);
        if (member == null) {
            return null;
        }
        writeMemberToCache(member);
        return convertToVO(member);
    }

    @Override
    public MemberSimpleVO identify(MemberIdentityDTO dto) {
        if (!StringUtils.hasText(dto.getPhone()) && !StringUtils.hasText(dto.getMemberCode())) {
            throw new BusinessException("手机号和会员码至少填写一个");
        }

        Member member = null;
        if (StringUtils.hasText(dto.getMemberCode())) {
            String codeKey = RedisKeyUtil.memberCode(dto.getMemberCode());
            String memberIdStr = stringRedisTemplate.opsForValue().get(codeKey);
            if (StringUtils.hasText(memberIdStr)) {
                member = memberMapper.selectById(Long.parseLong(memberIdStr));
            } else {
                LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Member::getMemberCode, dto.getMemberCode());
                wrapper.isNull(Member::getMergedTo);
                member = memberMapper.selectOne(wrapper);
            }
        }

        if (member == null && StringUtils.hasText(dto.getPhone())) {
            String phoneKey = RedisKeyUtil.phone(dto.getPhone());
            String memberIdStr = stringRedisTemplate.opsForValue().get(phoneKey);
            if (StringUtils.hasText(memberIdStr)) {
                member = memberMapper.selectById(Long.parseLong(memberIdStr));
            } else {
                LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Member::getPhone, dto.getPhone());
                wrapper.isNull(Member::getMergedTo);
                member = memberMapper.selectOne(wrapper);
            }
        }

        if (member == null) {
            return null;
        }
        writeMemberToCache(member);
        return convertToSimpleVO(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberVO update(MemberUpdateDTO dto) {
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        if (dto.getName() != null) {
            member.setName(dto.getName());
        }
        if (dto.getNickname() != null) {
            member.setNickname(dto.getNickname());
        }
        if (dto.getGender() != null) {
            member.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            member.setBirthday(dto.getBirthday());
        }
        if (dto.getAvatar() != null) {
            member.setAvatar(dto.getAvatar());
        }
        memberMapper.updateById(member);

        evictMemberCache(member.getId(), member.getPhone(), member.getMemberCode());
        writeMemberToCache(member);

        return convertToVO(member);
    }

    @Override
    public IPage<MemberVO> pageQuery(MemberQueryDTO dto) {
        Page<Member> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getPhone())) {
            wrapper.like(Member::getPhone, dto.getPhone());
        }
        if (StringUtils.hasText(dto.getMemberCode())) {
            wrapper.like(Member::getMemberCode, dto.getMemberCode());
        }
        if (StringUtils.hasText(dto.getName())) {
            wrapper.like(Member::getName, dto.getName());
        }
        if (dto.getLevelCode() != null) {
            wrapper.eq(Member::getLevelCode, dto.getLevelCode());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Member::getStatus, dto.getStatus());
        }
        wrapper.isNull(Member::getMergedTo);
        wrapper.orderByDesc(Member::getCreateTime);

        IPage<Member> memberPage = memberMapper.selectPage(page, wrapper);

        return memberPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MergeResultVO mergeMembers(MemberMergeDTO dto) {
        Long sourceId = dto.getSourceMemberId();
        Long targetId = dto.getTargetMemberId();

        if (sourceId.equals(targetId)) {
            throw new BusinessException("不能合并同一个会员");
        }

        Member source = memberMapper.selectById(sourceId);
        Member target = memberMapper.selectById(targetId);
        if (source == null) {
            throw new BusinessException("被合并会员不存在");
        }
        if (target == null) {
            throw new BusinessException("目标会员不存在");
        }
        if (source.getMergedTo() != null) {
            throw new BusinessException("被合并会员已被合并，不能重复合并");
        }
        if (target.getMergedTo() != null) {
            throw new BusinessException("目标会员已被合并，不能作为目标");
        }

        Member current = target;
        while (current.getMergedTo() != null) {
            if (current.getMergedTo().equals(sourceId)) {
                throw new BusinessException("检测到循环合并，操作已取消");
            }
            current = memberMapper.selectById(current.getMergedTo());
        }

        String lockKey = MERGE_LOCK_PREFIX + sourceId + "_" + targetId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("合并操作进行中，请稍后重试");
        }

        try {
            int mergedPoints = source.getCurrentPoints() != null ? source.getCurrentPoints() : 0;
            int mergedGrowth = source.getGrowthValue() != null ? source.getGrowthValue() : 0;
            int mergedCoupons = 0;

            // TODO: 调用优惠券模块迁移优惠券，统计数量
            // mergedCoupons = couponFacade.transferCoupons(sourceId, targetId);

            int targetCurrentPoints = (target.getCurrentPoints() != null ? target.getCurrentPoints() : 0) + mergedPoints;
            int targetTotalPoints = (target.getTotalPoints() != null ? target.getTotalPoints() : 0) + mergedPoints;
            int targetGrowth = (target.getGrowthValue() != null ? target.getGrowthValue() : 0) + mergedGrowth;

            target.setCurrentPoints(targetCurrentPoints);
            target.setTotalPoints(targetTotalPoints);
            target.setGrowthValue(targetGrowth);
            MemberLevelEnum newLevel = MemberLevelEnum.getLevelByGrowth(targetGrowth);
            target.setLevelCode(newLevel.getCode());
            memberMapper.updateById(target);

            source.setCurrentPoints(0);
            source.setGrowthValue(0);
            source.setMergedTo(targetId);
            memberMapper.updateById(source);

            String mergeNo = "MRG" + System.currentTimeMillis() +
                    String.format("%04d", (int) (Math.random() * 10000));
            MemberMergeLog mergeLog = new MemberMergeLog();
            mergeLog.setMergeNo(mergeNo);
            mergeLog.setSourceMemberId(sourceId);
            mergeLog.setTargetMemberId(targetId);
            mergeLog.setMergedPoints(mergedPoints);
            mergeLog.setMergedGrowth(mergedGrowth);
            mergeLog.setMergedCoupons(mergedCoupons);
            mergeLog.setOperator(dto.getOperator());
            mergeLog.setReason(dto.getReason());
            mergeLog.setCreateTime(LocalDateTime.now());
            memberMergeLogMapper.insert(mergeLog);

            evictMemberCache(sourceId, source.getPhone(), source.getMemberCode());
            evictMemberCache(targetId, target.getPhone(), target.getMemberCode());

            MergeResultVO result = new MergeResultVO();
            result.setMergeNo(mergeNo);
            result.setSourceMemberId(sourceId);
            result.setTargetMemberId(targetId);
            result.setMergedPoints(mergedPoints);
            result.setMergedGrowth(mergedGrowth);
            result.setMergedCoupons(mergedCoupons);
            return result;
        } finally {
            try {
                String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.warn("释放合并锁失败，key={}", lockKey, e);
            }
        }
    }

    @Override
    public void evictMemberCache(Long memberId, String phone, String memberCode) {
        if (memberId != null) {
            String idKey = MEMBER_ID_PREFIX + memberId;
            stringRedisTemplate.delete(idKey);
        }
        if (StringUtils.hasText(phone)) {
            String phoneKey = RedisKeyUtil.phone(phone);
            stringRedisTemplate.delete(phoneKey);
        }
        if (StringUtils.hasText(memberCode)) {
            String codeKey = RedisKeyUtil.memberCode(memberCode);
            stringRedisTemplate.delete(codeKey);
        }
    }

    private void writeMemberToCache(Member member) {
        if (member == null || member.getId() == null) {
            return;
        }
        String idKey = MEMBER_ID_PREFIX + member.getId();
        try {
            String json = objectMapper.writeValueAsString(member);
            stringRedisTemplate.opsForValue().set(idKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("序列化会员缓存失败，memberId={}", member.getId(), e);
        }

        if (StringUtils.hasText(member.getPhone())) {
            String phoneKey = RedisKeyUtil.phone(member.getPhone());
            stringRedisTemplate.opsForValue().set(phoneKey, member.getId().toString(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        if (StringUtils.hasText(member.getMemberCode())) {
            String codeKey = RedisKeyUtil.memberCode(member.getMemberCode());
            stringRedisTemplate.opsForValue().set(codeKey, member.getId().toString(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private MemberVO convertToVO(Member member) {
        if (member == null) {
            return null;
        }
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setMemberCode(member.getMemberCode());
        vo.setPhone(member.getPhone());
        vo.setName(member.getName());
        vo.setNickname(member.getNickname());
        vo.setGender(member.getGender());
        vo.setBirthday(member.getBirthday());
        vo.setAvatar(member.getAvatar());
        vo.setLevelCode(member.getLevelCode());
        vo.setGrowthValue(member.getGrowthValue());
        vo.setCurrentPoints(member.getCurrentPoints());
        vo.setTotalPoints(member.getTotalPoints());
        vo.setRegisterSource(member.getRegisterSource());
        vo.setStatus(member.getStatus());
        vo.setMergedTo(member.getMergedTo());
        vo.setRemark(member.getRemark());
        vo.setCreateTime(member.getCreateTime());
        vo.setUpdateTime(member.getUpdateTime());

        MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
        vo.setLevelName(levelEnum.getName());
        vo.setBenefitDesc(levelEnum.getDesc());
        return vo;
    }

    private MemberSimpleVO convertToSimpleVO(Member member) {
        if (member == null) {
            return null;
        }
        MemberSimpleVO vo = new MemberSimpleVO();
        vo.setId(member.getId());
        vo.setMemberCode(member.getMemberCode());
        vo.setPhone(member.getPhone());
        vo.setName(member.getName());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setLevelCode(member.getLevelCode());

        MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
        vo.setLevelName(levelEnum.getName());
        return vo;
    }

    @Override
    public MemberMergePreviewVO previewMerge(MemberMergePreviewDTO dto) {
        Long sourceId = dto.getSourceMemberId();
        Long targetId = dto.getTargetMemberId();

        if (sourceId.equals(targetId)) {
            throw new BusinessException("不能合并同一个会员");
        }

        Member source = memberMapper.selectById(sourceId);
        Member target = memberMapper.selectById(targetId);
        if (source == null) {
            throw new BusinessException("被合并会员不存在");
        }
        if (target == null) {
            throw new BusinessException("目标会员不存在");
        }
        if (source.getMergedTo() != null) {
            throw new BusinessException("被合并会员已被合并，不能重复合并");
        }
        if (target.getMergedTo() != null) {
            throw new BusinessException("目标会员已被合并，不能作为目标");
        }

        MemberMergePreviewVO.MergeMemberInfoVO sourceInfo = buildMemberInfo(source);
        MemberMergePreviewVO.MergeMemberInfoVO targetInfo = buildMemberInfo(target);

        MemberMergePreviewVO.MergeDiffSummaryVO diffSummary = buildDiffSummary(sourceInfo, targetInfo);

        MemberMergePreviewVO.MergeResultPreviewVO mergePreview = buildMergePreview(sourceInfo, targetInfo);

        List<String> warnings = buildWarnings(source, target, sourceInfo, targetInfo);

        MemberMergePreviewVO result = new MemberMergePreviewVO();
        result.setSourceMember(sourceInfo);
        result.setTargetMember(targetInfo);
        result.setDiffSummary(diffSummary);
        result.setMergePreview(mergePreview);
        result.setWarnings(warnings);
        return result;
    }

    @Override
    public IPage<MergeLogVO> pageMergeLogs(MemberMergeLogQueryDTO dto) {
        Page<MemberMergeLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<MemberMergeLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getOperator())) {
            wrapper.like(MemberMergeLog::getOperator, dto.getOperator());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(MemberMergeLog::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(MemberMergeLog::getCreateTime, dto.getEndTime());
        }

        List<Long> sourceMemberIds = null;
        List<Long> targetMemberIds = null;

        if (StringUtils.hasText(dto.getSourcePhone())) {
            LambdaQueryWrapper<Member> sourceWrapper = new LambdaQueryWrapper<>();
            sourceWrapper.like(Member::getPhone, dto.getSourcePhone());
            sourceWrapper.select(Member::getId);
            List<Member> sourceMembers = memberMapper.selectList(sourceWrapper);
            if (!sourceMembers.isEmpty()) {
                sourceMemberIds = sourceMembers.stream().map(Member::getId).toList();
            } else {
                sourceMemberIds = List.of(-1L);
            }
        }
        if (StringUtils.hasText(dto.getTargetPhone())) {
            LambdaQueryWrapper<Member> targetWrapper = new LambdaQueryWrapper<>();
            targetWrapper.like(Member::getPhone, dto.getTargetPhone());
            targetWrapper.select(Member::getId);
            List<Member> targetMembers = memberMapper.selectList(targetWrapper);
            if (!targetMembers.isEmpty()) {
                targetMemberIds = targetMembers.stream().map(Member::getId).toList();
            } else {
                targetMemberIds = List.of(-1L);
            }
        }
        if (sourceMemberIds != null) {
            wrapper.in(MemberMergeLog::getSourceMemberId, sourceMemberIds);
        }
        if (targetMemberIds != null) {
            wrapper.in(MemberMergeLog::getTargetMemberId, targetMemberIds);
        }

        wrapper.orderByDesc(MemberMergeLog::getCreateTime);

        IPage<MemberMergeLog> logPage = memberMergeLogMapper.selectPage(page, wrapper);

        return logPage.convert(this::convertToMergeLogVO);
    }

    @Override
    public MergeLogVO getMergeLog(Long mergeLogId) {
        if (mergeLogId == null) {
            return null;
        }
        MemberMergeLog log = memberMergeLogMapper.selectById(mergeLogId);
        if (log == null) {
            return null;
        }
        return convertToMergeLogVO(log);
    }

    private MemberMergePreviewVO.MergeMemberInfoVO buildMemberInfo(Member member) {
        MemberMergePreviewVO.MergeMemberInfoVO info = new MemberMergePreviewVO.MergeMemberInfoVO();
        info.setMemberId(member.getId());
        info.setMemberCode(member.getMemberCode());
        info.setPhone(member.getPhone());
        info.setName(member.getName());
        info.setNickname(member.getNickname());
        info.setAvatar(member.getAvatar());
        info.setBirthday(member.getBirthday());
        info.setRegisterTime(member.getCreateTime());
        info.setRegisterSource(member.getRegisterSource());
        info.setStatus(member.getStatus());
        info.setStatusName(member.getStatus() != null && member.getStatus() == 1 ? "正常" : "禁用");
        info.setLevelCode(member.getLevelCode());
        MemberLevelEnum levelEnum = MemberLevelEnum.getByCode(member.getLevelCode());
        info.setLevelName(levelEnum.getName());
        info.setGrowthValue(member.getGrowthValue() != null ? member.getGrowthValue() : 0);
        info.setCurrentPoints(member.getCurrentPoints() != null ? member.getCurrentPoints() : 0);
        info.setTotalPoints(member.getTotalPoints() != null ? member.getTotalPoints() : 0);

        LambdaQueryWrapper<CouponInstance> couponWrapper = new LambdaQueryWrapper<>();
        couponWrapper.eq(CouponInstance::getMemberId, member.getId());
        List<CouponInstance> allCoupons = couponInstanceMapper.selectList(couponWrapper);
        info.setTotalCouponCount(allCoupons.size());

        long availableCount = allCoupons.stream()
                .filter(c -> CouponStatusEnum.AVAILABLE.getCode().equals(c.getCouponStatus()))
                .count();
        info.setAvailableCouponCount((int) availableCount);

        LambdaQueryWrapper<ConsumeOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ConsumeOrder::getMemberId, member.getId());
        orderWrapper.in(ConsumeOrder::getOrderStatus,
                OrderStatusEnum.COMPLETED.getCode(),
                OrderStatusEnum.PAID.getCode());
        List<ConsumeOrder> orders = consumeOrderMapper.selectList(orderWrapper);
        info.setTotalConsumeCount(orders.size());

        BigDecimal totalAmount = orders.stream()
                .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        info.setTotalConsumeAmount(totalAmount);

        LocalDateTime lastConsumeTime = orders.stream()
                .map(ConsumeOrder::getCompleteTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        info.setLastConsumeTime(lastConsumeTime);

        return info;
    }

    private MemberMergePreviewVO.MergeDiffSummaryVO buildDiffSummary(
            MemberMergePreviewVO.MergeMemberInfoVO source,
            MemberMergePreviewVO.MergeMemberInfoVO target) {

        MemberMergePreviewVO.MergeDiffSummaryVO diff = new MemberMergePreviewVO.MergeDiffSummaryVO();

        boolean phoneDiff = !Objects.equals(source.getPhone(), target.getPhone());
        diff.setPhoneDiff(phoneDiff);

        if (!Objects.equals(source.getLevelCode(), target.getLevelCode())) {
            diff.setLevelDiff(source.getLevelName() + "→" + target.getLevelName());
        }
        diff.setPointsDiff(source.getCurrentPoints());
        diff.setCouponDiff(source.getAvailableCouponCount());
        diff.setGrowthDiff(source.getGrowthValue());
        diff.setConsumeDiff(Math.abs(source.getTotalConsumeCount() - target.getTotalConsumeCount()));

        List<String> dupInfo = new ArrayList<>();
        if (Objects.equals(source.getName(), target.getName()) && StringUtils.hasText(source.getName())) {
            dupInfo.add("姓名相同");
        }
        if (!Objects.equals(source.getBirthday(), target.getBirthday())
                && source.getBirthday() != null && target.getBirthday() != null) {
            dupInfo.add("生日不同");
        }
        if (!dupInfo.isEmpty()) {
            diff.setDuplicateInfo(String.join("/", dupInfo));
        }
        return diff;
    }

    private MemberMergePreviewVO.MergeResultPreviewVO buildMergePreview(
            MemberMergePreviewVO.MergeMemberInfoVO source,
            MemberMergePreviewVO.MergeMemberInfoVO target) {

        MemberMergePreviewVO.MergeResultPreviewVO preview = new MemberMergePreviewVO.MergeResultPreviewVO();

        int pointsToTransfer = source.getCurrentPoints();
        int growthToTransfer = source.getGrowthValue();
        int couponsToTransfer = source.getAvailableCouponCount();

        int finalPoints = target.getCurrentPoints() + pointsToTransfer;
        int finalGrowth = target.getGrowthValue() + growthToTransfer;
        int totalCouponsAfter = target.getAvailableCouponCount() + couponsToTransfer;
        int totalConsumeCountAfter = target.getTotalConsumeCount() + source.getTotalConsumeCount();
        BigDecimal totalConsumeAmountAfter = target.getTotalConsumeAmount()
                .add(source.getTotalConsumeAmount());

        MemberLevelEnum newLevel = MemberLevelEnum.getLevelByGrowth(finalGrowth);

        preview.setFinalPoints(finalPoints);
        preview.setFinalGrowth(finalGrowth);
        preview.setFinalLevelCode(newLevel.getCode());
        preview.setFinalLevelName(newLevel.getName());
        preview.setTotalCouponsAfter(totalCouponsAfter);
        preview.setTotalConsumeCountAfter(totalConsumeCountAfter);
        preview.setTotalConsumeAmountAfter(totalConsumeAmountAfter);
        preview.setPointsToTransfer(pointsToTransfer);
        preview.setCouponsToTransfer(couponsToTransfer);
        preview.setGrowthToTransfer(growthToTransfer);
        preview.setDescription("合并后将自动清零source会员并标记为已合并状态");

        return preview;
    }

    private List<String> buildWarnings(Member source, Member target,
                                       MemberMergePreviewVO.MergeMemberInfoVO sourceInfo,
                                       MemberMergePreviewVO.MergeMemberInfoVO targetInfo) {
        List<String> warnings = new ArrayList<>();

        if (!Objects.equals(source.getPhone(), target.getPhone())) {
            warnings.add("合并后将统一为目标手机号" + target.getPhone());
        }

        LambdaQueryWrapper<CouponInstance> lockedCouponWrapper = new LambdaQueryWrapper<>();
        lockedCouponWrapper.eq(CouponInstance::getMemberId, source.getId());
        lockedCouponWrapper.eq(CouponInstance::getCouponStatus, CouponStatusEnum.LOCKED.getCode());
        Long lockedCouponCount = couponInstanceMapper.selectCount(lockedCouponWrapper);
        if (lockedCouponCount != null && lockedCouponCount > 0) {
            warnings.add("被合并方存在" + lockedCouponCount + "张锁定优惠券，无法迁移");
        }

        if (sourceInfo.getLevelCode() != null && targetInfo.getLevelCode() != null
                && sourceInfo.getLevelCode() > targetInfo.getLevelCode()) {
            warnings.add("被合并方等级(" + sourceInfo.getLevelName()
                    + ")高于目标方等级(" + targetInfo.getLevelName() + ")，请确认");
        }

        if (source.getStatus() != null && source.getStatus() == 0) {
            warnings.add("被合并方会员状态为禁用，请确认");
        }

        return warnings;
    }

    private MergeLogVO convertToMergeLogVO(MemberMergeLog log) {
        if (log == null) {
            return null;
        }
        MergeLogVO vo = new MergeLogVO();
        vo.setId(log.getId());
        vo.setMergeNo(log.getMergeNo());
        vo.setSourceMemberId(log.getSourceMemberId());
        vo.setTargetMemberId(log.getTargetMemberId());
        vo.setMergedPoints(log.getMergedPoints());
        vo.setMergedGrowth(log.getMergedGrowth());
        vo.setMergedCoupons(log.getMergedCoupons());
        vo.setOperator(log.getOperator());
        vo.setOperatorName(log.getOperator());
        vo.setReason(log.getReason());
        vo.setCreateTime(log.getCreateTime());
        if (log.getCreateTime() != null) {
            vo.setCreateTimeFmt(log.getCreateTime().format(DATE_TIME_FORMATTER));
        }

        Member source = memberMapper.selectById(log.getSourceMemberId());
        if (source != null) {
            vo.setSourcePhone(source.getPhone());
            vo.setSourceName(source.getName());
        }
        Member target = memberMapper.selectById(log.getTargetMemberId());
        if (target != null) {
            vo.setTargetPhone(target.getPhone());
            vo.setTargetName(target.getName());
        }

        return vo;
    }
}
