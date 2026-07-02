package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.model.request.AiChatRequest;
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
    void Feign返回错误时客户端抛出中文异常()
    {
        StubRemoteKbService remoteKbService = new StubRemoteKbService();
        remoteKbService.fail = true;
        KbClient kbClient = new KbClient(remoteKbService);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> kbClient.hybridSearch("电脑无法联网", 3));

        assertEquals("知识库检索失败", exception.getMessage());
    }

    private static class StubRemoteKbService implements RemoteKbService
    {
        private KbSearchRequest lastRequest;

        private boolean fail;

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
    }

    private static class StubRemoteAiProxyService implements RemoteAiProxyService
    {
        private AiChatRequest lastChatRequest;

        @Override
        public R<AiChatResponse> chat(AiChatRequest request)
        {
            lastChatRequest = request;
            AiChatResponse response = new AiChatResponse();
            response.setConversationId(request.getConversationId());
            response.setContent("AI回答");
            return R.ok(response);
        }
    }
}
