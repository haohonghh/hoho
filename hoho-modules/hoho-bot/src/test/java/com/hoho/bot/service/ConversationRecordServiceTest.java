package com.hoho.bot.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.bot.domain.BotConversation;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.mapper.BotConversationMapper;
import com.hoho.bot.mapper.BotMessageMapper;
import com.hoho.bot.model.response.BotChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationRecordServiceTest
{
    @Test
    void 首次记录用户消息时创建会话并写入消息()
    {
        StubConversationMapper conversationMapper = new StubConversationMapper();
        StubMessageMapper messageMapper = new StubMessageMapper();
        ConversationRecordService service = new ConversationRecordService(conversationMapper, messageMapper);

        service.recordUserMessage("session-001", "电脑无法联网");

        assertEquals("session-001", conversationMapper.inserted.getConversationId());
        assertEquals("电脑无法联网", conversationMapper.inserted.getTitle());
        assertEquals("user", messageMapper.insertedMessages.get(0).getRole());
        assertEquals("电脑无法联网", messageMapper.insertedMessages.get(0).getContent());
    }

    @Test
    void 记录机器人消息时更新会话摘要并写入来源和分数()
    {
        StubConversationMapper conversationMapper = new StubConversationMapper();
        conversationMapper.existing = new BotConversation();
        conversationMapper.existing.setConversationId("session-001");
        StubMessageMapper messageMapper = new StubMessageMapper();
        ConversationRecordService service = new ConversationRecordService(conversationMapper, messageMapper);
        BotChatResponse response = new BotChatResponse();
        response.setAnswer("请检查网线");
        response.setSource("kb");
        response.setScore(0.82D);

        service.recordAssistantMessage("session-001", response);

        assertEquals("assistant", messageMapper.insertedMessages.get(0).getRole());
        assertEquals("kb", messageMapper.insertedMessages.get(0).getSource());
        assertEquals(0.82D, messageMapper.insertedMessages.get(0).getScore());
        assertEquals("请检查网线", conversationMapper.updated.getLastMessage());
    }

    @Test
    void 可以查询会话列表和消息列表()
    {
        StubConversationMapper conversationMapper = new StubConversationMapper();
        StubMessageMapper messageMapper = new StubMessageMapper();
        ConversationRecordService service = new ConversationRecordService(conversationMapper, messageMapper);

        assertEquals(conversationMapper.conversations, service.listConversations());
        assertEquals(messageMapper.messages, service.listMessages("session-001"));
    }

    private static class StubConversationMapper implements BotConversationMapper
    {
        private BotConversation existing;

        private BotConversation inserted;

        private BotConversation updated;

        private final List<BotConversation> conversations = List.of(new BotConversation());

        @Override
        public BotConversation selectByConversationId(String conversationId)
        {
            return existing;
        }

        @Override
        public List<BotConversation> selectConversationList()
        {
            return conversations;
        }

        @Override
        public int insertConversation(BotConversation conversation)
        {
            inserted = conversation;
            existing = conversation;
            return 1;
        }

        @Override
        public int updateConversationSummary(BotConversation conversation)
        {
            updated = conversation;
            return 1;
        }
    }

    private static class StubMessageMapper implements BotMessageMapper
    {
        private final List<BotMessage> insertedMessages = new ArrayList<>();

        private final List<BotMessage> messages = List.of(new BotMessage());

        @Override
        public int insertMessage(BotMessage message)
        {
            insertedMessages.add(message);
            return 1;
        }

        @Override
        public List<BotMessage> selectMessageList(String conversationId)
        {
            return messages;
        }
    }
}
