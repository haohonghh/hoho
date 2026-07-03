package com.hoho.bot.api;

import com.hoho.bot.api.factory.RemoteAiProxyFallbackFactory;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.request.AiLongTermMemoryUpsertRequest;
import com.hoho.bot.model.request.AiLongTermMemoryProfileQueryRequest;
import com.hoho.bot.model.request.AiMemoryAppendRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.LongTermMemoryProfileResponse;
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

    /**
     * 追加AI短期记忆。
     *
     * @param request 短期记忆追加请求
     * @return 追加结果
     */
    @PostMapping("/ai/memory/append")
    R<Void> appendMemory(@RequestBody AiMemoryAppendRequest request);

    /**
     * 写入AI长期记忆。
     *
     * @param request 长期记忆写入请求
     * @return 写入结果
     */
    @PostMapping("/ai/memory/long-term/upsert")
    R<Void> upsertLongTermMemory(@RequestBody AiLongTermMemoryUpsertRequest request);

    /**
     * 查询AI长期记忆画像。
     *
     * @param request 长期记忆画像查询请求
     * @return 画像结果
     */
    @PostMapping("/ai/memory/long-term/profile")
    R<LongTermMemoryProfileResponse> profile(@RequestBody AiLongTermMemoryProfileQueryRequest request);
}
