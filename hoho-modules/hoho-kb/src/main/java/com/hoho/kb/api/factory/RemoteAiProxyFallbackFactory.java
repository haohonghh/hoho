package com.hoho.kb.api.factory;

import com.hoho.common.core.domain.R;
import com.hoho.kb.api.RemoteAiProxyService;
import com.hoho.kb.model.request.AiEmbeddingRequest;
import com.hoho.kb.model.response.AiEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * AI代理远程服务降级处理
 *
 * @author hoho
 */
@Component
public class RemoteAiProxyFallbackFactory implements FallbackFactory<RemoteAiProxyService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteAiProxyFallbackFactory.class);

    @Override
    public RemoteAiProxyService create(Throwable throwable)
    {
        log.error("AI代理服务调用失败:{}", throwable.getMessage());
        return new RemoteAiProxyService()
        {
            @Override
            public R<AiEmbeddingResponse> embedding(AiEmbeddingRequest request)
            {
                return R.fail("AI代理向量化失败:" + throwable.getMessage());
            }
        };
    }
}
