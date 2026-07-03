package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.request.AiLongTermMemoryUpsertRequest;
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
import reactor.core.publisher.Flux;

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
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

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
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

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
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

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
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

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
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setConversationId(null);

        BotChatResponse response = botService.chat(request);

        assertTrue(response.getConversationId() != null && !response.getConversationId().isBlank());
        assertEquals(response.getConversationId(), conversationRecordService.lastConversationId);
    }

    @Test
    void 流式对话复用普通对话链路并写入会话和消息记录()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        List<String> chunks = botService.streamChat(request()).collectList().block();

        assertEquals(List.of("检查网线和 Wi-Fi。"), chunks);
        assertEquals("test-session", conversationRecordService.lastConversationId);
        assertEquals("电脑上不了网怎么处理？", conversationRecordService.lastUserMessage);
        assertEquals("检查网线和 Wi-Fi。", conversationRecordService.lastAssistantAnswer);
    }

    @Test
    void 流式Ai分片结束后会拼接完整答案并写入消息记录()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.62D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubAiProxyStreamClient aiProxyStreamClient = new StubAiProxyStreamClient();
        aiProxyStreamClient.chunks = List.of("请先检查网线，", "再确认 Wi-Fi 和 DNS。");
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, aiProxyStreamClient, properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        List<String> chunks = botService.streamChat(request()).collectList().block();

        assertEquals(List.of("请先检查网线，", "再确认 Wi-Fi 和 DNS。"), chunks);
        assertEquals("test-session", conversationRecordService.lastConversationId);
        assertEquals("电脑上不了网怎么处理？", conversationRecordService.lastUserMessage);
        assertEquals("请先检查网线，再确认 Wi-Fi 和 DNS。", conversationRecordService.lastAssistantAnswer);
    }

    @Test
    void 知识库检索异常时降级走Ai兜底()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        kbClient.throwOnSearch = true;
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.responseContent = "请先确认网络连接状态。";
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        BotChatResponse response = botService.chat(request());

        assertEquals("ai", response.getSource());
        assertEquals("请先确认网络连接状态。", response.getAnswer());
        assertEquals(1, aiProxyClient.chatCount);
        assertEquals(properties.getAnswer().getFallbackSystemPrompt(), aiProxyClient.lastSystemPrompt);
    }

    @Test
    void Ai调用异常时返回固定服务降级话术()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.38D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.throwOnChat = true;
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        BotChatResponse response = botService.chat(request());

        assertEquals("fallback", response.getSource());
        assertEquals(properties.getAnswer().getServiceDegradeReply(), response.getAnswer());
        assertEquals(properties.getAnswer().getServiceDegradeReply(), conversationRecordService.lastAssistantAnswer);
    }

    @Test
    void 追加短期记忆失败时不影响知识库答案返回()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        aiProxyClient.throwOnAppendMemory = true;
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        BotChatResponse response = botService.chat(request());

        assertEquals("kb", response.getSource());
        assertEquals("检查网线和 Wi-Fi。", response.getAnswer());
        assertEquals(1, aiProxyClient.memoryAppendCount);
        assertEquals("检查网线和 Wi-Fi。", conversationRecordService.lastAssistantAnswer);
    }

    @Test
    void 明确表达回答偏好时自动写入长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "回答风格", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("以后都用中文，回答简洁一点。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals(0L, aiProxyClient.lastLongTermMemoryUserId);
        assertEquals("preference", aiProxyClient.lastLongTermMemoryType);
        assertEquals("reply_style", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("以后都用中文，回答简洁一点。", aiProxyClient.lastLongTermMemoryValue);
    }

    @Test
    void 普通问题不写入长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "电脑无法联网怎么办？", "检查网线和 Wi-Fi。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));

        botService.chat(request());

        assertEquals(0, aiProxyClient.longTermMemoryUpsertCount);
    }

    @Test
    void 明确表达语言偏好时写入回复语言长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "语言偏好", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("之后请都用中文回复我。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals("preference", aiProxyClient.lastLongTermMemoryType);
        assertEquals("reply_language", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("中文", aiProxyClient.lastLongTermMemoryValue);
    }

    @Test
    void 明确表达分步骤偏好时写入回复风格长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "风格偏好", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("以后回答请分步骤说明。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals("preference", aiProxyClient.lastLongTermMemoryType);
        assertEquals("reply_style", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("分步骤", aiProxyClient.lastLongTermMemoryValue);
    }

    @Test
    void 明确表达用户身份时写入角色长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "用户身份", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("我是运维工程师。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals("profile", aiProxyClient.lastLongTermMemoryType);
        assertEquals("role", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("运维", aiProxyClient.lastLongTermMemoryValue);
    }

    @Test
    void 明确表达操作系统环境时写入环境长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "环境信息", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("我们公司电脑都是 Windows。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals("environment", aiProxyClient.lastLongTermMemoryType);
        assertEquals("os", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("Windows", aiProxyClient.lastLongTermMemoryValue);
    }

    @Test
    void 明确表达代理网络环境时写入网络长期记忆()
    {
        BotProperties properties = botProperties();
        StubKbClient kbClient = new StubKbClient(properties, item(1L, "网络环境", "收到。", 0.82D));
        StubAiProxyClient aiProxyClient = new StubAiProxyClient(properties);
        StubConversationRecordService conversationRecordService = new StubConversationRecordService();
        BotService botService = new BotService(kbClient, aiProxyClient, new StubAiProxyStreamClient(), properties,
                conversationRecordService, new StubLongTermMemoryCaptureService(aiProxyClient));
        BotChatRequest request = request();
        request.setMessage("我们公司网络需要走代理。");

        botService.chat(request);

        assertEquals(1, aiProxyClient.longTermMemoryUpsertCount);
        assertEquals("environment", aiProxyClient.lastLongTermMemoryType);
        assertEquals("network", aiProxyClient.lastLongTermMemoryKey);
        assertEquals("代理网络", aiProxyClient.lastLongTermMemoryValue);
    }

    private BotProperties botProperties()
    {
        BotProperties properties = new BotProperties();
        properties.getAnswer().setDirectMinScore(0.7D);
        properties.getAnswer().setAssistMinScore(0.5D);
        properties.getAnswer().setTopK(3);
        properties.getAnswer().setServiceDegradeReply("当前智能服务暂时不可用，请稍后重试或联系人工运维。");
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

        private boolean throwOnSearch;

        StubKbClient(BotProperties properties, KbSearchItem item)
        {
            super(new StubRemoteKbService(item));
            this.item = item;
        }

        @Override
        public KbSearchResponse hybridSearch(String query, int topK)
        {
            if (throwOnSearch)
            {
                throw new IllegalStateException("知识库服务异常");
            }
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

        private boolean throwOnChat;

        private boolean throwOnAppendMemory;

        private int longTermMemoryUpsertCount;

        private Long lastLongTermMemoryUserId;

        private String lastLongTermMemoryType;

        private String lastLongTermMemoryKey;

        private String lastLongTermMemoryValue;

        StubAiProxyClient(BotProperties properties)
        {
            super(new StubRemoteAiProxyService());
        }

        @Override
        public AiChatResponse chat(String conversationId, String systemPrompt, String message)
        {
            if (throwOnChat)
            {
                throw new IllegalStateException("AI代理对话失败");
            }
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
            if (throwOnAppendMemory)
            {
                throw new IllegalStateException("AI短期记忆追加失败");
            }
        }

        @Override
        public void upsertLongTermMemory(Long userId, String conversationId, String memoryType, String memoryKey, String memoryValue)
        {
            longTermMemoryUpsertCount++;
            lastLongTermMemoryUserId = userId;
            lastLongTermMemoryType = memoryType;
            lastLongTermMemoryKey = memoryKey;
            lastLongTermMemoryValue = memoryValue;
        }
    }

    private static class StubAiProxyStreamClient extends AiProxyStreamClient
    {
        private List<String> chunks = List.of("AI 回答");

        StubAiProxyStreamClient()
        {
            super(org.springframework.web.reactive.function.client.WebClient.builder());
        }

        @Override
        public Flux<String> streamChat(String conversationId, String systemPrompt, String message)
        {
            return Flux.fromIterable(chunks);
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

    private static class StubLongTermMemoryCaptureService extends LongTermMemoryCaptureService
    {
        StubLongTermMemoryCaptureService(AiProxyClient aiProxyClient)
        {
            super(new BotUserContext(), aiProxyClient);
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

        @Override
        public R<Void> upsertLongTermMemory(AiLongTermMemoryUpsertRequest request)
        {
            return R.ok();
        }
    }
}
