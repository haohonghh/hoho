package com.hoho.bot.service;

import com.hoho.bot.api.RemoteAiProxyService;
import com.hoho.bot.model.request.AiChatRequest;
import com.hoho.bot.model.request.AiLongTermMemoryProfileQueryRequest;
import com.hoho.bot.model.request.AiLongTermMemoryUpsertRequest;
import com.hoho.bot.model.request.AiMemoryAppendRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.LongTermMemoryProfileResponse;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI代理客户端
 *
 * @author hoho
 */
@Component
public class AiProxyClient
{
    private static final Logger log = LoggerFactory.getLogger(AiProxyClient.class);

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
    public AiChatResponse chat(String conversationId, String systemPrompt, String message, String scene, String agentCode)
    {
        AiChatRequest request = new AiChatRequest();
        request.setConversationId(conversationId);
        request.setSystemPrompt(systemPrompt);
        request.setMessage(message);
        request.setScene(scene);
        request.setAgentCode(agentCode);
        // temperature 设为 0.3，使回答更稳定、更贴近知识库资料，减少随意发挥
        request.setTemperature(0.3D);

        long start = System.currentTimeMillis();
        log.info("调用AI代理对话开始 conversationId={}, agentCode={}, scene={}, messageLength={}, systemPromptLength={}", conversationId,
                agentCode, scene, length(message), length(systemPrompt));
        R<AiChatResponse> response = remoteAiProxyService.chat(request);
        if (response == null || R.isError(response))
        {
            log.warn("调用AI代理对话失败 conversationId={}, cost={}ms, reason={}", conversationId,
                    System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "AI代理对话无响应" : response.getMsg());
        }
        log.info("调用AI代理对话完成 conversationId={}, model={}, outputLength={}, cost={}ms", conversationId,
                response.getData() == null ? null : response.getData().getModel(),
                response.getData() == null ? 0 : length(response.getData().getContent()),
                System.currentTimeMillis() - start);
        return response.getData();
    }

    /**
     * 追加未经过模型调用的短期记忆。
     *
     * @param conversationId  会话编号
     * @param userMessage     用户消息
     * @param assistantAnswer 助手回复
     */
    public void appendMemory(String conversationId, String userMessage, String assistantAnswer)
    {
        AiMemoryAppendRequest request = new AiMemoryAppendRequest();
        request.setConversationId(conversationId);
        request.setUserMessage(userMessage);
        request.setAssistantMessage(assistantAnswer);

        long start = System.currentTimeMillis();
        log.info("追加AI短期记忆开始 conversationId={}, userMessageLength={}, assistantAnswerLength={}", conversationId,
                length(userMessage), length(assistantAnswer));
        R<Void> response = remoteAiProxyService.appendMemory(request);
        if (response == null || R.isError(response))
        {
            log.warn("追加AI短期记忆失败 conversationId={}, cost={}ms, reason={}", conversationId,
                    System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "AI短期记忆追加无响应" : response.getMsg());
        }
        log.info("追加AI短期记忆完成 conversationId={}, cost={}ms", conversationId, System.currentTimeMillis() - start);
    }

    /**
     * 写入长期记忆。
     *
     * @param userId         用户编号
     * @param conversationId 会话编号
     * @param memoryType     记忆类型
     * @param memoryKey      记忆键
     * @param memoryValue    记忆内容
     */
    public void upsertLongTermMemory(Long userId, String conversationId, String memoryType, String memoryKey, String memoryValue)
    {
        AiLongTermMemoryUpsertRequest request = new AiLongTermMemoryUpsertRequest();
        request.setUserId(userId);
        request.setConversationId(conversationId);
        request.setMemoryType(memoryType);
        request.setMemoryKey(memoryKey);
        request.setMemoryValue(memoryValue);

        long start = System.currentTimeMillis();
        log.info("写入AI长期记忆开始 userId={}, conversationId={}, memoryType={}, memoryKey={}",
                userId, conversationId, memoryType, memoryKey);
        R<Void> response = remoteAiProxyService.upsertLongTermMemory(request);
        if (response == null || R.isError(response))
        {
            log.warn("写入AI长期记忆失败 userId={}, conversationId={}, cost={}ms, reason={}",
                    userId, conversationId, System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "AI长期记忆写入无响应" : response.getMsg());
        }
        log.info("写入AI长期记忆完成 userId={}, conversationId={}, cost={}ms",
                userId, conversationId, System.currentTimeMillis() - start);
    }

    public LongTermMemoryProfileResponse profile(Long userId)
    {
        AiLongTermMemoryProfileQueryRequest request = new AiLongTermMemoryProfileQueryRequest();
        request.setUserId(userId);

        long start = System.currentTimeMillis();
        log.info("查询AI长期记忆画像开始 userId={}", userId);
        R<LongTermMemoryProfileResponse> response = remoteAiProxyService.profile(request);
        if (response == null || R.isError(response))
        {
            log.warn("查询AI长期记忆画像失败 userId={}, cost={}ms, reason={}",
                    userId, System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "AI长期记忆画像查询无响应" : response.getMsg());
        }
        log.info("查询AI长期记忆画像完成 userId={}, hasPreferences={}, hasProfile={}, hasEnvironment={}, cost={}ms",
                userId,
                response.getData() != null && response.getData().getPreferences() != null && !response.getData().getPreferences().isEmpty(),
                response.getData() != null && response.getData().getProfile() != null && !response.getData().getProfile().isEmpty(),
                response.getData() != null && response.getData().getEnvironment() != null && !response.getData().getEnvironment().isEmpty(),
                System.currentTimeMillis() - start);
        return response.getData();
    }

    private int length(String value)
    {
        return value == null ? 0 : value.length();
    }
}
