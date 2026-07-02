package com.hoho.bot.service;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.common.core.domain.R;
import org.springframework.stereotype.Component;

/**
 * AI代理客户端
 *
 * @author hoho
 */
@Component
public class AiProxyClient
{
    private final RemoteAiProxyService remoteAiProxyService;

    public AiProxyClient(RemoteAiProxyService remoteAiProxyService)
    {
        this.remoteAiProxyService = remoteAiProxyService;
    }

    /**
     * 调用 AI 代理服务进行单次对话。
     *
     * <p>底层通过 Feign 调用 {@code hoho-ai-proxy} 的 {@code POST /ai/chat} 接口，
     * 请求体会携带会话编号、system prompt、用户消息以及固定的 temperature=0.3
     * （temperature 取值较低以保证回答的确定性与稳定性）。
     *
     * <p>响应统一使用 {@code R<T>} 信封结构体包装。当 HTTP 层成功
     * 但业务层返回错误或 body 为 null 时，抛出 {@link IllegalStateException}，
     * 错误信息来自信封的 {@code msg} 字段，便于上层感知具体的失败原因。
     *
     * @param conversationId 会话编号，由 BotService 提供（新建或延续），
     *                       用于 ai-proxy 侧的对话上下文与会话管理
     * @param systemPrompt   系统提示词，告诉 AI 角色定位与回答策略；
     *                       BotService 会根据不同路由路径注入不同的内容
     * @param message        当前轮次的用户输入文本
     * @return AiChatResponse 包含 AI 生成的正文（content）、使用的模型等信息；
     *         永远不会返回 null，失败时会抛出异常
     * @throws IllegalStateException 当响应为 null 或业务状态为 error 时
     */
    public AiChatResponse chat(String conversationId, String systemPrompt, String message)
    {
        AiChatRequest request = new AiChatRequest();
        request.setConversationId(conversationId);
        request.setSystemPrompt(systemPrompt);
        request.setMessage(message);
        // temperature 设为 0.3，使回答更稳定、更贴近知识库资料，减少随意发挥
        request.setTemperature(0.3D);

        R<AiChatResponse> response = remoteAiProxyService.chat(request);
        if (response == null || R.isError(response))
        {
            throw new IllegalStateException(response == null ? "AI代理对话无响应" : response.getMsg());
        }
        return response.getData();
    }
}
