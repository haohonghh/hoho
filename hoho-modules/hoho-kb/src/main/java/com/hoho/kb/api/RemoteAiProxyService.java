package com.hoho.kb.api;

import com.hoho.common.core.domain.R;
import com.hoho.kb.api.factory.RemoteAiProxyFallbackFactory;
import com.hoho.kb.model.request.AiEmbeddingRequest;
import com.hoho.kb.model.response.AiEmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI代理远程服务
 *
 * @author hoho
 */
@FeignClient(contextId = "kbRemoteAiProxyService", value = "hoho-ai-proxy",
        fallbackFactory = RemoteAiProxyFallbackFactory.class)
public interface RemoteAiProxyService
{
    /**
     * 调用AI代理向量化接口。
     *
     * @param request 向量化请求
     * @return 向量化结果
     */
    @PostMapping("/ai/embedding")
    R<AiEmbeddingResponse> embedding(@RequestBody AiEmbeddingRequest request);
}
