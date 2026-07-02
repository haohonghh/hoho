package com.hoho.bot.api;

import com.hoho.bot.api.factory.RemoteAiProxyFallbackFactory;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI代理远程服务
 *
 * @author hoho
 */
@FeignClient(contextId = "botRemoteAiProxyService", value = "hoho-ai-proxy",
        fallbackFactory = RemoteAiProxyFallbackFactory.class)
public interface RemoteAiProxyService
{
    /**
     * 调用AI代理对话接口。
     *
     * @param request 对话请求
     * @return 对话结果
     */
    @PostMapping("/ai/chat")
    R<AiChatResponse> chat(@RequestBody AiChatRequest request);
}
