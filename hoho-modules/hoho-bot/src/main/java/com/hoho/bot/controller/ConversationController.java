package com.hoho.bot.controller;

import java.util.List;

import com.hoho.bot.domain.BotConversation;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.service.ConversationRecordService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话记录接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/bot/conversation")
public class ConversationController
{
    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationRecordService conversationRecordService;

    public ConversationController(ConversationRecordService conversationRecordService)
    {
        this.conversationRecordService = conversationRecordService;
    }

    @GetMapping("/list")
    public R<List<BotConversation>> list()
    {
        try
        {
            return R.ok(conversationRecordService.listConversations());
        }
        catch (Exception e)
        {
            log.error("查询会话列表失败", e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{conversationId}/messages")
    public R<List<BotMessage>> messages(@PathVariable String conversationId)
    {
        try
        {
            return R.ok(conversationRecordService.listMessages(conversationId));
        }
        catch (Exception e)
        {
            log.error("查询会话消息失败", e);
            return R.fail(e.getMessage());
        }
    }
}
