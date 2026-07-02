package com.hoho.bot.api.factory;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.common.core.domain.R;
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
            public R<AiChatResponse> chat(AiChatRequest request)
            {
                return R.fail("AI代理对话失败:" + throwable.getMessage());
            }
        };
    }
}
