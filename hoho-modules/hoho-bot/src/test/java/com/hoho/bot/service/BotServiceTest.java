package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.config.BotProperties;
import com.hoho.bot.memory.BotConversationMemoryService;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.model.response.KbSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

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
        StubBotConversationMemoryService memoryService = new StubBotConversationMemoryService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService, memoryService);

        BotChatResponse response = botService.chat(request());

        assertEquals("kb", response.getSource());
        assertEquals("检查网线和 Wi-Fi。", response.getAnswer());
        assertEquals(0.82D, response.getScore());
        assertEquals(0, aiProxyClient.chatCount);
        assertEquals("电脑上不了网怎么处理？", conversationRecordService.lastUserMessage);
        assertEquals("检查网线和 Wi-Fi。", conversationRecordService.lastAssistantAnswer);
        assertEquals("电脑上不了网怎么处理？", memoryService.lastUserMessage);
        assertEquals("检查网线和 Wi-Fi。", memoryService.lastAssistantMessage);
    }

    @Test
    void 中分命中时基于知识库辅助生成答案()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.62D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "请先检查网线和 Wi-Fi，再确认 IP 与 DNS。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        StubBotConversationMemoryService memoryService = new StubBotConversationMemoryService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService, memoryService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai_with_kb", response.getSource());
        assertEquals("请先检查网线和 Wi-Fi，再确认 IP 与 DNS。", response.getAnswer());
        assertEquals(0.62D, response.getScore());
        assertEquals(1, aiProxyClient.chatCount);
        assertTrue(aiProxyClient.lastSystemPrompt.contains("电脑无法联网怎么办？"));
        assertTrue(aiProxyClient.lastSystemPrompt.contains("检查网线和 Wi-Fi。"));
        assertEquals("请先检查网线和 Wi-Fi，再确认 IP 与 DNS。", conversationRecordService.lastAssistantAnswer);
        assertEquals("请先检查网线和 Wi-Fi，再确认 IP 与 DNS。", memoryService.lastAssistantMessage);
    }

    @Test
    void 低分命中时使用普通兜底提示词()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.38D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "请提供更多问题现象。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        StubBotConversationMemoryService memoryService = new StubBotConversationMemoryService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService, memoryService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai", response.getSource());
        assertEquals("请提供更多问题现象。", response.getAnswer());
        assertNull(response.getScore());
        assertEquals(1, aiProxyClient.chatCount);
        assertEquals(properties.getAnswer().getFallbackSystemPrompt(), aiProxyClient.lastSystemPrompt);
        assertEquals("请提供更多问题现象。", conversationRecordService.lastAssistantAnswer);
    }

    @Test
    void 调用Ai时将短期记忆拼入系统提示词并记录本轮消息()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "打印机问题", "重启打印机。", 0.2D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "可以继续检查网关配置。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        StubBotConversationMemoryService memoryService = new StubBotConversationMemoryService();
        memoryService.memoryPrompt = "用户：上一轮问题\n助手：上一轮回答";
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService, memoryService);

        BotChatResponse response = botService.chat(request());

        assertEquals("ai", response.getSource());
        assertTrue(aiProxyClient.lastSystemPrompt.contains(properties.getAnswer().getFallbackSystemPrompt()));
        assertTrue(aiProxyClient.lastSystemPrompt.contains("用户：上一轮问题"));
        assertTrue(aiProxyClient.lastSystemPrompt.contains("助手：上一轮回答"));
        assertEquals("电脑上不了网怎么处理？", memoryService.lastUserMessage);
        assertEquals("可以继续检查网关配置。", memoryService.lastAssistantMessage);
    }

    @Test
    void 未传会话编号时自动生成会话编号并记录消息()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        StubBotConversationMemoryService memoryService = new StubBotConversationMemoryService();
        BotService botService = new BotService(kbClient, aiProxyClient, properties, conversationRecordService, memoryService);
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
            super(new RestTemplate(), properties);
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

        StubAiProxyClient(BotProperties properties)
        {
            super(new RestTemplate(), properties);
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
    }

    private static class StubConversationRecordService extends ConversationRecordService
    {
        private String lastConversationId;

        private String lastUserMessage;

        private String lastAssistantAnswer;

        StubConversationRecordService()
        {
            super(null, null);
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

    private static class StubBotConversationMemoryService extends BotConversationMemoryService
    {
        private String memoryPrompt = "";

        private String lastConversationId;

        private String lastUserMessage;

        private String lastAssistantMessage;

        StubBotConversationMemoryService()
        {
            super(null, new BotProperties());
        }

        @Override
        public String buildMemoryPrompt(String conversationId)
        {
            lastConversationId = conversationId;
            return memoryPrompt;
        }

        @Override
        public void recordUserMessage(String conversationId, String message)
        {
            lastConversationId = conversationId;
            lastUserMessage = message;
        }

        @Override
        public void recordAssistantMessage(String conversationId, String message)
        {
            lastConversationId = conversationId;
            lastAssistantMessage = message;
        }
    }
}
