package com.hoho.kb.service;

import java.util.List;

import com.hoho.common.core.domain.R;
import com.hoho.kb.api.RemoteAiProxyService;
import com.hoho.kb.model.request.AiEmbeddingRequest;
import com.hoho.kb.model.response.AiEmbeddingResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KbFeignClientTest
{
    @Test
    void 向量化客户端通过Feign接口调用Ai代理()
    {
        StubRemoteAiProxyService remoteAiProxyService = new StubRemoteAiProxyService();
        AiProxyClient aiProxyClient = new AiProxyClient(remoteAiProxyService);

        List<Double> vector = aiProxyClient.embedding("网络故障");

        assertEquals("网络故障", remoteAiProxyService.lastRequest.getText());
        assertEquals(List.of(0.1D, 0.2D), vector);
    }

    @Test
    void 向量化客户端收到错误响应时抛出中文异常()
    {
        StubRemoteAiProxyService remoteAiProxyService = new StubRemoteAiProxyService();
        remoteAiProxyService.fail = true;
        AiProxyClient aiProxyClient = new AiProxyClient(remoteAiProxyService);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> aiProxyClient.embedding("网络故障"));

        assertEquals("AI代理向量化失败", exception.getMessage());
    }

    private static class StubRemoteAiProxyService implements RemoteAiProxyService
    {
        private AiEmbeddingRequest lastRequest;

        private boolean fail;

        @Override
        public R<AiEmbeddingResponse> embedding(AiEmbeddingRequest request)
        {
            lastRequest = request;
            if (fail)
            {
                return R.fail("AI代理向量化失败");
            }

            AiEmbeddingResponse response = new AiEmbeddingResponse();
            response.setVector(List.of(0.1D, 0.2D));
            return R.ok(response);
        }
    }
}
