package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.request.AiMemoryAppendRequest;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.request.KbQaRequest;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotServiceTest
{
    @Test
    void 高分命中时直接返回知识库答案()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService);

        BotChatResponse response = botService.chat(request());

        assertEquals("kb", response.getSource());
        assertEquals("检查网线和 Wi-Fi。", response.getAnswer());
        assertEquals(0.82D, response.getScore());
        assertEquals(0, aiProxyClient.chatCount);
        assertEquals("电脑上不了网怎么处理？", conversationRecordService.lastUserMessage);
        assertEquals("检查网线和 Wi-Fi。", conversationRecordService.lastAssistantAnswer);
        assertEquals("电脑上不了网怎么处理？", aiProxyClient.lastMemoryUserMessage);
        assertEquals("检查网线和 Wi-Fi。", aiProxyClient.lastMemoryAssistantMessage);
    }

    @Test
    void 中分命中时基于知识库辅助生成答案()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.62D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "请先检查网线和 Wi-Fi，再确认 IP 与 DNS。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai_with_kb", response.getSource());
        assertEquals("请先检查网线和 Wi-Fi，再确认 IP 与 DNS。", response.getAnswer());
        assertEquals(0.62D, response.getScore());
        assertEquals(1, aiProxyClient.chatCount);
        assertTrue(aiProxyClient.lastSystemPrompt.contains("电脑无法联网怎么办？"));
        assertTrue(aiProxyClient.lastSystemPrompt.contains("检查网线和 Wi-Fi。"));
        assertEquals("请先检查网线和 Wi-Fi，再确认 IP 与 DNS。", conversationRecordService.lastAssistantAnswer);
        assertEquals(0, aiProxyClient.memoryAppendCount);
    }

    @Test
    void 低分命中时使用普通兜底提示词()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.38D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "请提供更多问题现象。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai", response.getSource());
        assertEquals("请提供更多问题现象。", response.getAnswer());
        assertNull(response.getScore());
        assertEquals(1, aiProxyClient.chatCount);
        assertEquals(properties.getAnswer().getFallbackSystemPrompt(), aiProxyClient.lastSystemPrompt);
        assertEquals("请提供更多问题现象。", conversationRecordService.lastAssistantAnswer);
        assertEquals(0, aiProxyClient.memoryAppendCount);
    }

    @Test
    void 调用Ai时不再将短期记忆拼入系统提示词()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "打印机问题", "重启打印机。", 0.2D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "可以继续检查网关配置。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai", response.getSource());
        assertEquals(properties.getAnswer().getFallbackSystemPrompt(), aiProxyClient.lastSystemPrompt);
        assertEquals(0, aiProxyClient.memoryAppendCount);
    }

    @Test
    void 未传会话编号时自动生成会话编号并记录消息()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService);
        BotChatRequest request = request();
        request.setConversationId(null);

        BotChatResponse response = botService.chat(request);

        assertTrue(response.getConversationId() != null && !response.getConversationId().isBlank());
        assertEquals(response.getConversationId(), conversationRecordService.lastConversationId);
    }

    private BotProperties botProperties()
    {
        BotProperties properties = new BotProperties();
        properties.getAnswer().setDirectMinScore(0.7D);
        properties.getAnswer().setAssistMinScore(0.5D);
        properties.getAnswer().setTopK(3);
        return properties;
    }

    private BotChatRequest request()
    {
        BotChatRequest request = new BotChatRequest();
        request.setConversationId("test-session");
        request.setMessage("电脑上不了网怎么处理？");
        return request;
    }

    private KbSearchItem item(Long qaId, String question, String answer, Double score)
    {
        KbSearchItem item = new KbSearchItem();
        item.setQaId(qaId);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setScore(score);
        item.setSource("hybrid");
        return item;
    }

    private static class StubKbClient extends KbClient
    {
        private final KbSearchItem item;

        StubKbClient(BotProperties properties, KbSearchItem item)
        {
            super(new StubRemoteKbService(item));
            this.item = item;
        }

        @Override
        public KbSearchResponse hybridSearch(String query, int topK)
        {
            KbSearchResponse response = new KbSearchResponse();
            response.setQuery(query);
            response.setItems(List.of(item));
            return response;
        }
    }

    private static class StubAiProxyClient extends AiProxyClient
    {
        private int chatCount;

        private String responseContent = "AI 回答";

        private String lastSystemPrompt;

        private int memoryAppendCount;

        private String lastMemoryUserMessage;

        private String lastMemoryAssistantMessage;

        StubAiProxyClient(BotProperties properties)
        {
            super(new StubRemoteAiProxyService());
        }

        @Override
        public AiChatResponse chat(String conversationId, String systemPrompt, String message)
        {
            chatCount++;
            lastSystemPrompt = systemPrompt;
            AiChatResponse response = new AiChatResponse();
            response.setConversationId(conversationId);
            response.setContent(responseContent);
            response.setModel("test-model");
            return response;
        }

        @Override
        public void appendMemory(String conversationId, String userMessage, String assistantAnswer)
        {
            memoryAppendCount++;
            lastMemoryUserMessage = userMessage;
            lastMemoryAssistantMessage = assistantAnswer;
        }
    }

    private static class StubConversationRecordService extends ConversationRecordService
    {
        private String lastConversationId;

        private String lastUserMessage;

        private String lastAssistantAnswer;

        StubConversationRecordService()
        {
            super(null, null, new BotUserContext());
        }

        @Override
        public void recordUserMessage(String conversationId, String content)
        {
            lastConversationId = conversationId;
            lastUserMessage = content;
        }

        @Override
        public void recordAssistantMessage(String conversationId, BotChatResponse response)
        {
            lastConversationId = conversationId;
            lastAssistantAnswer = response.getAnswer();
        }
    }

    private static class StubRemoteKbService implements RemoteKbService
    {
        private final KbSearchItem item;

        StubRemoteKbService(KbSearchItem item)
        {
            this.item = item;
        }

        @Override
        public R<KbSearchResponse> hybridSearch(KbSearchRequest request)
        {
            KbSearchResponse response = new KbSearchResponse();
            response.setQuery(request.getQuery());
            response.setItems(List.of(item));
            return R.ok(response);
        }

        @Override
        public R<Long> createQa(KbQaRequest request)
        {
            return R.ok(1L);
        }

        @Override
        public R<Boolean> publishQa(Long id)
        {
            return R.ok(true);
        }
    }

    private static class StubRemoteAiProxyService implements RemoteAiProxyService
    {
        @Override
        public R<AiChatResponse> chat(AiChatRequest request)
        {
            AiChatResponse response = new AiChatResponse();
            response.setConversationId(request.getConversationId());
            response.setContent("AI 回答");
            response.setModel("test-model");
            return R.ok(response);
        }

        @Override
        public R<Void> appendMemory(AiMemoryAppendRequest request)
        {
            return R.ok();
        }
    }
}
