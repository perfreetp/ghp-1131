package com.smartretail.mbc.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.message.dto.ExpireReminderDTO;
import com.smartretail.mbc.message.dto.MessageBatchDTO;
import com.smartretail.mbc.message.dto.MessageQueryDTO;
import com.smartretail.mbc.message.dto.MessageSendDTO;
import com.smartretail.mbc.message.service.MessageService;
import com.smartretail.mbc.message.vo.MessageVO;
import com.smartretail.mbc.message.vo.SendResultVO;
import com.smartretail.mbc.message.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息触达模块")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送单条消息", description = "发送单条消息给指定会员，支持站内信、短信、微信、APP推送等渠道")
    @PostMapping("/send")
    public Result<SendResultVO> sendMessage(
            @Parameter(description = "发送消息请求", required = true)
            @Valid @RequestBody MessageSendDTO dto) {
        return Result.success(messageService.sendMessage(dto));
    }

    @Operation(summary = "批量发送消息", description = "批量发送相同内容的消息给多个会员")
    @PostMapping("/batch-send")
    public Result<List<SendResultVO>> batchSend(
            @Parameter(description = "批量发送消息请求", required = true)
            @Valid @RequestBody MessageBatchDTO dto) {
        return Result.success(messageService.batchSend(dto));
    }

    @Operation(summary = "分页查询消息", description = "按会员、类型、状态、渠道、时间范围等条件分页查询消息记录")
    @PostMapping("/query")
    public Result<IPage<MessageVO>> queryMessages(
            @Parameter(description = "消息查询请求", required = true)
            @RequestBody MessageQueryDTO dto) {
        return Result.success(messageService.queryMessages(dto));
    }

    @Operation(summary = "标记单条消息已读", description = "将指定消息标记为已读状态")
    @PutMapping("/read/{messageId}")
    public Result<Void> markRead(
            @Parameter(description = "消息ID", required = true)
            @PathVariable Long messageId) {
        messageService.markRead(messageId);
        return Result.success();
    }

    @Operation(summary = "标记所有消息已读", description = "将会员所有未读消息标记为已读状态")
    @PutMapping("/read-all/{memberId}")
    public Result<Void> markAllRead(
            @Parameter(description = "会员ID", required = true)
            @PathVariable Long memberId) {
        messageService.markAllRead(memberId);
        return Result.success();
    }

    @Operation(summary = "获取未读消息计数", description = "获取会员未读消息总数及按类型分组的未读数")
    @GetMapping("/unread/{memberId}")
    public Result<UnreadCountVO> getUnreadCount(
            @Parameter(description = "会员ID", required = true)
            @PathVariable Long memberId) {
        return Result.success(messageService.getUnreadCount(memberId));
    }

    @Operation(summary = "手动触发到期提醒", description = "手动触发券到期提醒或积分到期提醒推送")
    @PostMapping("/reminder/expire")
    public Result<Integer> pushExpireReminders(
            @Parameter(description = "到期提醒请求", required = true)
            @Valid @RequestBody ExpireReminderDTO dto) {
        int count;
        if (dto.getReminderType() == 1) {
            count = messageService.pushCouponExpireReminders(dto.getDays());
        } else if (dto.getReminderType() == 2) {
            count = messageService.pushPointExpireReminders(dto.getDays());
        } else {
            return Result.fail("不支持的提醒类型");
        }
        return Result.success(count);
    }
}
