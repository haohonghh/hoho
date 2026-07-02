package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.domain.BotConversation;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.mapper.BotConversationMapper;
import com.hoho.bot.mapper.BotMessageMapper;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话记录服务
 *
 * @author hoho
 */
@Service
public class ConversationRecordService
{
    private final BotConversationMapper conversationMapper;

    private final BotMessageMapper messageMapper;

    public ConversationRecordService(BotConversationMapper conversationMapper, BotMessageMapper messageMapper)
    {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordUserMessage(String conversationId, String content)
    {
        ensureConversation(conversationId, content);
        insertMessage(conversationId, "user", content, null, null);
        updateSummary(conversationId, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordAssistantMessage(String conversationId, BotChatResponse response)
    {
        if (response == null)
        {
            return;
        }
        ensureConversation(conversationId, response.getAnswer());
        insertMessage(conversationId, "assistant", response.getAnswer(), response.getSource(), response.getScore());
        updateSummary(conversationId, response.getAnswer());
    }

    public List<BotConversation> listConversations()
    {
        return conversationMapper.selectConversationList();
    }

    public List<BotMessage> listMessages(String conversationId)
    {
        if (StringUtils.isBlank(conversationId))
        {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        return messageMapper.selectMessageList(conversationId);
    }

    private void ensureConversation(String conversationId, String title)
    {
        BotConversation existing = conversationMapper.selectByConversationId(conversationId);
        if (existing != null)
        {
            return;
        }
        BotConversation conversation = new BotConversation();
        conversation.setConversationId(conversationId);
        conversation.setTitle(buildTitle(title));
        conversation.setLastMessage("");
        conversationMapper.insertConversation(conversation);
    }

    private void insertMessage(String conversationId, String role, String content, String source, Double score)
    {
        BotMessage message = new BotMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSource(source);
        message.setScore(score);
        messageMapper.insertMessage(message);
    }

    private void updateSummary(String conversationId, String lastMessage)
    {
        BotConversation conversation = new BotConversation();
        conversation.setConversationId(conversationId);
        conversation.setLastMessage(lastMessage);
        conversationMapper.updateConversationSummary(conversation);
    }

    private String buildTitle(String content)
    {
        if (StringUtils.isBlank(content))
        {
            return "新会话";
        }
        String title = content.trim();
        return title.length() > 30 ? title.substring(0, 30) : title;
    }
}
