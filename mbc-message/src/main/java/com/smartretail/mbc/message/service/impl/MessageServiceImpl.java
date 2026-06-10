package com.smartretail.mbc.message.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartretail.mbc.common.enums.CouponStatusEnum;
import com.smartretail.mbc.common.enums.MessageTypeEnum;
import com.smartretail.mbc.common.enums.MemberLevelEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.coupon.entity.CouponInstance;
import com.smartretail.mbc.coupon.entity.CouponTemplate;
import com.smartretail.mbc.coupon.mapper.CouponInstanceMapper;
import com.smartretail.mbc.coupon.mapper.CouponTemplateMapper;
import com.smartretail.mbc.message.dto.MessageBatchDTO;
import com.smartretail.mbc.message.dto.MessageQueryDTO;
import com.smartretail.mbc.message.dto.MessageSendDTO;
import com.smartretail.mbc.message.entity.MessageLog;
import com.smartretail.mbc.message.mapper.MessageLogMapper;
import com.smartretail.mbc.message.service.MessageService;
import com.smartretail.mbc.message.vo.MessageVO;
import com.smartretail.mbc.message.vo.SendResultVO;
import com.smartretail.mbc.message.vo.UnreadCountVO;
import com.smartretail.mbc.point.entity.PointLog;
import com.smartretail.mbc.point.mapper.PointLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageLogMapper messageLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Lazy
    private CouponInstanceMapper couponInstanceMapper;

    @Autowired
    @Lazy
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    @Lazy
    private PointLogMapper pointLogMapper;

    private static final String MSG_NO_PREFIX = "MSG";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendResultVO sendMessage(MessageSendDTO dto) {
        String msgNo = generateMsgNo();
        MessageLog messageLog = new MessageLog();
        messageLog.setMsgNo(msgNo);
        messageLog.setMemberId(dto.getMemberId());
        messageLog.setMsgType(dto.getMsgType());
        messageLog.setMsgTitle(dto.getMsgTitle());
        messageLog.setMsgContent(dto.getMsgContent());
        messageLog.setChannel(StringUtils.hasText(dto.getChannel()) ? dto.getChannel() : "INNER");
        messageLog.setSendStatus(0);
        messageLog.setRetryCount(0);
        messageLog.setBizId(dto.getBizId());
        if (dto.getBizData() != null && !dto.getBizData().isEmpty()) {
            try {
                messageLog.setBizData(objectMapper.writeValueAsString(dto.getBizData()));
            } catch (JsonProcessingException e) {
                log.error("bizData序列化失败", e);
            }
        }
        messageLogMapper.insert(messageLog);

        SendResultVO result = new SendResultVO();
        result.setMsgNo(msgNo);
        result.setSuccess(true);
        result.setSendTime(LocalDateTime.now());

        doSend(messageLog.getId());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SendResultVO> batchSend(MessageBatchDTO dto) {
        List<SendResultVO> results = new ArrayList<>();
        if (CollectionUtils.isEmpty(dto.getMemberIds())) {
            return results;
        }
        for (Long memberId : dto.getMemberIds()) {
            MessageSendDTO sendDTO = new MessageSendDTO();
            sendDTO.setMemberId(memberId);
            sendDTO.setMsgType(dto.getMsgType());
            sendDTO.setMsgTitle(dto.getMsgTitle());
            sendDTO.setMsgContent(dto.getMsgContent());
            sendDTO.setChannel(dto.getChannel());
            sendDTO.setBizId(dto.getBizId());
            sendDTO.setBizData(dto.getBizData());
            try {
                SendResultVO result = sendMessage(sendDTO);
                results.add(result);
            } catch (Exception e) {
                SendResultVO failResult = new SendResultVO();
                failResult.setMsgNo(generateMsgNo());
                failResult.setSuccess(false);
                failResult.setFailReason(e.getMessage());
                failResult.setSendTime(LocalDateTime.now());
                results.add(failResult);
            }
        }
        return results;
    }

    @Override
    public IPage<MessageVO> queryMessages(MessageQueryDTO dto) {
        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();
        if (dto.getMemberId() != null) {
            wrapper.eq(MessageLog::getMemberId, dto.getMemberId());
        }
        if (dto.getMsgType() != null) {
            wrapper.eq(MessageLog::getMsgType, dto.getMsgType());
        }
        if (dto.getSendStatus() != null) {
            wrapper.eq(MessageLog::getSendStatus, dto.getSendStatus());
        }
        if (StringUtils.hasText(dto.getChannel())) {
            wrapper.eq(MessageLog::getChannel, dto.getChannel());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(MessageLog::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(MessageLog::getCreateTime, dto.getEndTime());
        }
        wrapper.orderByDesc(MessageLog::getCreateTime);

        Page<MessageLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<MessageLog> logPage = messageLogMapper.selectPage(page, wrapper);

        Page<MessageVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        List<MessageVO> voList = logPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long messageId) {
        MessageLog messageLog = messageLogMapper.selectById(messageId);
        if (messageLog == null) {
            throw new BusinessException("消息不存在");
        }
        LambdaUpdateWrapper<MessageLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MessageLog::getId, messageId)
                .in(MessageLog::getSendStatus, Arrays.asList(0, 1))
                .set(MessageLog::getSendStatus, 3)
                .set(MessageLog::getReadTime, LocalDateTime.now());
        messageLogMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long memberId) {
        LambdaUpdateWrapper<MessageLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MessageLog::getMemberId, memberId)
                .in(MessageLog::getSendStatus, Arrays.asList(0, 1))
                .set(MessageLog::getSendStatus, 3)
                .set(MessageLog::getReadTime, LocalDateTime.now());
        messageLogMapper.update(null, updateWrapper);
    }

    @Override
    public UnreadCountVO getUnreadCount(Long memberId) {
        UnreadCountVO vo = new UnreadCountVO();
        vo.setMemberId(memberId);
        int total = messageLogMapper.countUnreadByMemberId(memberId);
        vo.setTotalUnread(total);
        List<Map<String, Object>> groupList = messageLogMapper.countUnreadGroupByType(memberId);
        Map<Integer, Integer> countsByType = new HashMap<>();
        if (!CollectionUtils.isEmpty(groupList)) {
            for (Map<String, Object> map : groupList) {
                Integer msgType = map.get("msgType") != null ? ((Number) map.get("msgType")).intValue() : null;
                Integer cnt = map.get("cnt") != null ? ((Number) map.get("cnt")).intValue() : 0;
                if (msgType != null) {
                    countsByType.put(msgType, cnt);
                }
            }
        }
        vo.setCountsByType(countsByType);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pushCouponExpireReminders(Integer days) {
        int count = 0;
        List<Integer> statusList = Arrays.asList(
                CouponStatusEnum.NOT_STARTED.getCode(),
                CouponStatusEnum.AVAILABLE.getCode(),
                CouponStatusEnum.LOCKED.getCode()
        );
        List<CouponInstance> expiringList = couponInstanceMapper.selectExpiringInDays(days, statusList);
        if (CollectionUtils.isEmpty(expiringList)) {
            log.info("没有即将过期的优惠券");
            return 0;
        }
        log.info("查询到{}张即将过期的优惠券", expiringList.size());
        Set<Long> templateIds = expiringList.stream()
                .map(CouponInstance::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CouponTemplate> templateMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            List<CouponTemplate> templates = couponTemplateMapper.selectBatchIds(templateIds);
            templateMap = templates.stream()
                    .collect(Collectors.toMap(CouponTemplate::getId, t -> t, (a, b) -> a));
        }
        for (CouponInstance instance : expiringList) {
            try {
                long daysLeft = 1;
                if (instance.getValidEnd() != null) {
                    daysLeft = Math.max(1, Duration.between(LocalDateTime.now(), instance.getValidEnd()).toDays() + 1);
                }
                String couponName = "优惠券";
                CouponTemplate template = templateMap.get(instance.getTemplateId());
                if (template != null && StringUtils.hasText(template.getCouponName())) {
                    couponName = template.getCouponName();
                }
                MessageSendDTO sendDTO = new MessageSendDTO();
                sendDTO.setMemberId(instance.getMemberId());
                sendDTO.setMsgType(MessageTypeEnum.COUPON_EXPIRE.getCode());
                sendDTO.setMsgTitle("您的优惠券即将过期");
                sendDTO.setMsgContent(String.format("您的%s还有%d天过期，快去使用吧~", couponName, daysLeft));
                sendDTO.setChannel("INNER");
                sendDTO.setBizId(instance.getId());
                Map<String, Object> bizData = new HashMap<>();
                bizData.put("instanceId", instance.getId());
                bizData.put("templateId", instance.getTemplateId());
                bizData.put("couponName", couponName);
                bizData.put("validEnd", instance.getValidEnd() != null ? instance.getValidEnd().toString() : null);
                bizData.put("daysLeft", daysLeft);
                sendDTO.setBizData(bizData);
                sendMessage(sendDTO);
                count++;
            } catch (Exception e) {
                log.error("推送券到期提醒失败 - instanceId:{}", instance.getId(), e);
            }
        }
        log.info("券到期提醒推送完成，共推送{}条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pushPointExpireReminders(Integer days) {
        int count = 0;
        LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(PointLog::getExpireTime)
                .le(PointLog::getExpireTime, LocalDateTime.now().plusDays(days))
                .gt(PointLog::getExpireTime, LocalDateTime.now())
                .eq(PointLog::getPointType, 1)
                .gt(PointLog::getChangePoints, 0);
        List<PointLog> expiringLogs = pointLogMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(expiringLogs)) {
            log.info("没有即将过期的积分");
            return 0;
        }
        Map<Long, Integer> memberExpirePoints = new LinkedHashMap<>();
        Map<Long, LocalDateTime> memberMinExpireTime = new HashMap<>();
        for (PointLog pointLog : expiringLogs) {
            Long memberId = pointLog.getMemberId();
            int points = pointLog.getChangePoints() != null ? pointLog.getChangePoints() : 0;
            memberExpirePoints.merge(memberId, points, Integer::sum);
            LocalDateTime expireTime = pointLog.getExpireTime();
            if (expireTime != null) {
                if (!memberMinExpireTime.containsKey(memberId) || expireTime.isBefore(memberMinExpireTime.get(memberId))) {
                    memberMinExpireTime.put(memberId, expireTime);
                }
            }
        }
        log.info("查询到{}个会员有即将过期的积分", memberExpirePoints.size());
        for (Map.Entry<Long, Integer> entry : memberExpirePoints.entrySet()) {
            try {
                Long memberId = entry.getKey();
                Integer expirePoints = entry.getValue();
                long daysLeft = 1;
                LocalDateTime minExpireTime = memberMinExpireTime.get(memberId);
                if (minExpireTime != null) {
                    daysLeft = Math.max(1, Duration.between(LocalDateTime.now(), minExpireTime).toDays() + 1);
                }
                MessageSendDTO sendDTO = new MessageSendDTO();
                sendDTO.setMemberId(memberId);
                sendDTO.setMsgType(MessageTypeEnum.POINT_EXPIRE.getCode());
                sendDTO.setMsgTitle("您的积分即将过期");
                sendDTO.setMsgContent(String.format("您有%d积分将在%d天内过期，快去使用吧~", expirePoints, daysLeft));
                sendDTO.setChannel("INNER");
                Map<String, Object> bizData = new HashMap<>();
                bizData.put("expirePoints", expirePoints);
                bizData.put("daysLeft", daysLeft);
                bizData.put("minExpireTime", minExpireTime != null ? minExpireTime.toString() : null);
                sendDTO.setBizData(bizData);
                sendMessage(sendDTO);
                count++;
            } catch (Exception e) {
                log.error("推送积分到期提醒失败 - memberId:{}", entry.getKey(), e);
            }
        }
        log.info("积分到期提醒推送完成，共推送{}条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushLevelChangeMessage(Long memberId, Integer beforeLevel, Integer afterLevel) {
        String beforeLevelName = getLevelName(beforeLevel);
        String afterLevelName = getLevelName(afterLevel);
        boolean isUpgrade = afterLevel != null && beforeLevel != null && afterLevel > beforeLevel;
        MessageSendDTO sendDTO = new MessageSendDTO();
        sendDTO.setMemberId(memberId);
        sendDTO.setMsgType(MessageTypeEnum.LEVEL_CHANGE.getCode());
        sendDTO.setMsgTitle(isUpgrade ? "恭喜您会员等级升级啦" : "温馨提醒：您的会员等级已变更");
        sendDTO.setMsgContent(String.format("您的会员等级由%s变更为%s%s",
                beforeLevelName,
                afterLevelName,
                isUpgrade ? "，享受更多专属权益！" : ""));
        sendDTO.setChannel("INNER");
        Map<String, Object> bizData = new HashMap<>();
        bizData.put("beforeLevel", beforeLevel);
        bizData.put("afterLevel", afterLevel);
        bizData.put("beforeLevelName", beforeLevelName);
        bizData.put("afterLevelName", afterLevelName);
        bizData.put("isUpgrade", isUpgrade);
        sendDTO.setBizData(bizData);
        sendMessage(sendDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushCouponReceiveMessage(Long memberId, Long templateId, String couponName) {
        MessageSendDTO sendDTO = new MessageSendDTO();
        sendDTO.setMemberId(memberId);
        sendDTO.setMsgType(MessageTypeEnum.COUPON_RECEIVE.getCode());
        sendDTO.setMsgTitle("恭喜您成功领取优惠券");
        sendDTO.setMsgContent(String.format("您已成功领取【%s】，快去使用吧~", couponName));
        sendDTO.setChannel("INNER");
        sendDTO.setBizId(templateId);
        Map<String, Object> bizData = new HashMap<>();
        bizData.put("templateId", templateId);
        bizData.put("couponName", couponName);
        sendDTO.setBizData(bizData);
        sendMessage(sendDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushBirthdayBenefitMessage(Long memberId, Integer points, Integer couponCount) {
        MessageSendDTO sendDTO = new MessageSendDTO();
        sendDTO.setMemberId(memberId);
        sendDTO.setMsgType(MessageTypeEnum.BIRTHDAY_BENEFIT.getCode());
        sendDTO.setMsgTitle("祝您生日快乐，专属权益已发放");
        StringBuilder content = new StringBuilder("亲爱的会员，祝您生日快乐！");
        boolean hasBenefit = false;
        if (points != null && points > 0) {
            content.append(String.format("已为您赠送%d积分", points));
            hasBenefit = true;
        }
        if (couponCount != null && couponCount > 0) {
            if (hasBenefit) {
                content.append("，");
            }
            content.append(String.format("已为您发放%d张优惠券", couponCount));
            hasBenefit = true;
        }
        if (hasBenefit) {
            content.append("，快去查看使用吧~");
        }
        sendDTO.setMsgContent(content.toString());
        sendDTO.setChannel("INNER");
        Map<String, Object> bizData = new HashMap<>();
        bizData.put("points", points);
        bizData.put("couponCount", couponCount);
        sendDTO.setBizData(bizData);
        sendMessage(sendDTO);
    }

    @Async
    public void doSend(Long messageId) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        MessageLog messageLog = messageLogMapper.selectById(messageId);
        if (messageLog == null) {
            log.warn("消息不存在，发送中止 - messageId:{}", messageId);
            return;
        }
        try {
            String channel = messageLog.getChannel();
            if (!"INNER".equals(channel)) {
                log.info("模拟推送至{}: target={}, msgNo={}, title={}",
                        channel, messageLog.getTarget(), messageLog.getMsgNo(), messageLog.getMsgTitle());
            }
            boolean simulateFail = new Random().nextInt(100) < 2;
            if (simulateFail) {
                throw new RuntimeException("模拟推送失败");
            }
            LambdaUpdateWrapper<MessageLog> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MessageLog::getId, messageId)
                    .set(MessageLog::getSendStatus, 1)
                    .set(MessageLog::getSendTime, LocalDateTime.now());
            messageLogMapper.update(null, updateWrapper);
        } catch (Exception e) {
            log.error("消息发送失败 - messageId:{}, msgNo:{}", messageId, messageLog.getMsgNo(), e);
            LambdaUpdateWrapper<MessageLog> updateWrapper = new LambdaUpdateWrapper<>();
            int retryCount = messageLog.getRetryCount() != null ? messageLog.getRetryCount() : 0;
            updateWrapper.eq(MessageLog::getId, messageId)
                    .set(MessageLog::getSendStatus, 2)
                    .set(MessageLog::getRetryCount, retryCount + 1)
                    .set(MessageLog::getFailReason, e.getMessage());
            messageLogMapper.update(null, updateWrapper);
        }
    }

    private String generateMsgNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomPart = 1000 + new Random().nextInt(9000);
        return MSG_NO_PREFIX + timePart + randomPart;
    }

    private MessageVO convertToVO(MessageLog entity) {
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
        vo.setMsgNo(entity.getMsgNo());
        vo.setMemberId(entity.getMemberId());
        vo.setMsgType(entity.getMsgType());
        vo.setMsgTitle(entity.getMsgTitle());
        vo.setMsgContent(entity.getMsgContent());
        vo.setChannel(entity.getChannel());
        vo.setTarget(entity.getTarget());
        vo.setSendStatus(entity.getSendStatus());
        vo.setRetryCount(entity.getRetryCount());
        vo.setFailReason(entity.getFailReason());
        vo.setSendTime(entity.getSendTime());
        vo.setReadTime(entity.getReadTime());
        vo.setBizId(entity.getBizId());
        vo.setBizData(entity.getBizData());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String getLevelName(Integer levelCode) {
        if (levelCode == null) {
            return "未知等级";
        }
        for (MemberLevelEnum levelEnum : MemberLevelEnum.values()) {
            if (levelEnum.getCode().equals(levelCode)) {
                return levelEnum.getName();
            }
        }
        return "V" + levelCode;
    }
}
