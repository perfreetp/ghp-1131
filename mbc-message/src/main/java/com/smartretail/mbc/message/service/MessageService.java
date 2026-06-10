package com.smartretail.mbc.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.message.dto.ExpireReminderDTO;
import com.smartretail.mbc.message.dto.MessageBatchDTO;
import com.smartretail.mbc.message.dto.MessageQueryDTO;
import com.smartretail.mbc.message.dto.MessageSendDTO;
import com.smartretail.mbc.message.vo.MessageVO;
import com.smartretail.mbc.message.vo.SendResultVO;
import com.smartretail.mbc.message.vo.UnreadCountVO;

import java.util.List;

public interface MessageService {

    SendResultVO sendMessage(MessageSendDTO dto);

    List<SendResultVO> batchSend(MessageBatchDTO dto);

    IPage<MessageVO> queryMessages(MessageQueryDTO dto);

    void markRead(Long messageId);

    void markAllRead(Long memberId);

    UnreadCountVO getUnreadCount(Long memberId);

    int pushCouponExpireReminders(Integer days);

    int pushPointExpireReminders(Integer days);

    void pushLevelChangeMessage(Long memberId, Integer beforeLevel, Integer afterLevel);

    void pushCouponReceiveMessage(Long memberId, Long templateId, String couponName);

    void pushBirthdayBenefitMessage(Long memberId, Integer points, Integer couponCount);
}
