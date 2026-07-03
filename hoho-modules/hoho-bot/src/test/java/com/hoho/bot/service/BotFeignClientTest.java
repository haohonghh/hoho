package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.request.AiLongTermMemoryUpsertRequest;
import com.hoho.bot.model.request.AiMemoryAppendRequest;
import com.hoho.bot.model.request.KbQaRequest;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BotFeignClientTest
{
    @Test
    void 知识库客户端通过Feign接口发起混合检索()
    {
        StubRemoteKbService remoteKbService = new StubRemoteKbService();
        KbClient kbClient = new KbClient(remoteKbService);

        KbSearchResponse response = kbClient.hybridSearch("电脑无法联网", 3);

        assertEquals("电脑无法联网", remoteKbService.lastRequest.getQuery());
        assertEquals(3, remoteKbService.lastRequest.getTopK());
        assertEquals("hybrid", response.getItems().get(0).getSource());
    }

    @Test
    void 对话客户端通过Feign接口调用Ai代理()
    {
        StubRemoteAiProxyService remoteAiProxyService = new StubRemoteAiProxyService();
        AiProxyClient aiProxyClient = new AiProxyClient(remoteAiProxyService);

        AiChatResponse response = aiProxyClient.chat("session-1", "系统提示词", "用户问题");

        assertEquals("session-1", remoteAiProxyService.lastChatRequest.getConversationId());
        assertEquals("系统提示词", remoteAiProxyService.lastChatRequest.getSystemPrompt());
        assertEquals("用户问题", remoteAiProxyService.lastChatRequest.getMessage());
        assertEquals(0.3D, remoteAiProxyService.lastChatRequest.getTemperature());
        assertEquals("AI回答", response.getContent());
    }

    @Test
    void 对话客户端通过Feign接口追加短期记忆()
    {
        StubRemoteAiProxyService remoteAiProxyService = new StubRemoteAiProxyService();
        AiProxyClient aiProxyClient = new AiProxyClient(remoteAiProxyService);

        aiProxyClient.appendMemory("session-1", "用户问题", "助手回答");

        assertEquals("session-1", remoteAiProxyService.lastMemoryRequest.getConversationId());
        assertEquals("用户问题", remoteAiProxyService.lastMemoryRequest.getUserMessage());
        assertEquals("助手回答", remoteAiProxyService.lastMemoryRequest.getAssistantMessage());
    }

    @Test
    void Feign返回错误时客户端抛出中文异常()
    {
        StubRemoteKbService remoteKbService = new StubRemoteKbService();
        remoteKbService.fail = true;
        KbClient kbClient = new KbClient(remoteKbService);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> kbClient.hybridSearch("电脑无法联网", 3));

        assertEquals("知识库检索失败", exception.getMessage());
    }

    @Test
    void 知识库客户端可以创建并发布问答知识()
    {
        StubRemoteKbService remoteKbService = new StubRemoteKbService();
        KbClient kbClient = new KbClient(remoteKbService);

        Long qaId = kbClient.createAndPublishQa(2L, "电脑无法联网怎么办？", "先检查 DNS 配置。", null);

        assertEquals(7L, qaId);
        assertEquals(2L, remoteKbService.lastQaRequest.getCategoryId());
        assertEquals("电脑无法联网怎么办？", remoteKbService.lastQaRequest.getQuestion());
        assertEquals("先检查 DNS 配置。", remoteKbService.lastQaRequest.getAnswer());
        assertEquals(7L, remoteKbService.lastPublishId);
    }

    private static class StubRemoteKbService implements RemoteKbService
    {
        private KbSearchRequest lastRequest;

        private boolean fail;

        private KbQaRequest lastQaRequest;

        private Long lastPublishId;

        @Override
        public R<KbSearchResponse> hybridSearch(KbSearchRequest request)
        {
            lastRequest = request;
            if (fail)
            {
                return R.fail("知识库检索失败");
            }

            KbSearchItem item = new KbSearchItem();
            item.setSource("hybrid");
            KbSearchResponse response = new KbSearchResponse();
            response.setQuery(request.getQuery());
            response.setItems(List.of(item));
            return R.ok(response);
        }

        @Override
        public R<Long> createQa(KbQaRequest request)
        {
            lastQaRequest = request;
            return R.ok(7L);
        }

        @Override
        public R<Boolean> publishQa(Long id)
        {
            lastPublishId = id;
            return R.ok(true);
        }
    }

    private static class StubRemoteAiProxyService implements RemoteAiProxyService
    {
        private AiChatRequest lastChatRequest;

        private AiMemoryAppendRequest lastMemoryRequest;

        @Override
        public R<AiChatResponse> chat(AiChatRequest request)
        {
            lastChatRequest = request;
            AiChatResponse response = new AiChatResponse();
            response.setConversationId(request.getConversationId());
            response.setContent("AI回答");
            return R.ok(response);
        }

        @Override
        public R<Void> appendMemory(AiMemoryAppendRequest request)
        {
            lastMemoryRequest = request;
            return R.ok();
        }

        @Override
        public R<Void> upsertLongTermMemory(AiLongTermMemoryUpsertRequest request)
        {
            return R.ok();
        }
    }
}
