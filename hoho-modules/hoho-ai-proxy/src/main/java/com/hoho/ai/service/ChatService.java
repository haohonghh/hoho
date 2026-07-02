package com.hoho.ai.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.ChatRequest;
import com.hoho.ai.model.response.AiChatResponse;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * 对话服务
 *
 * @author hoho
 */
@Service
public class ChatService
{
    private final ChatModel chatModel;

    private final ChatClient chatClient;

    private final AiProxyProperties aiProxyProperties;

    public ChatService(ChatModel chatModel, ChatMemory chatMemory, AiProxyProperties aiProxyProperties)
    {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.aiProxyProperties = aiProxyProperties;
    }

    /**
     * 执行单轮对话调用。
     * <p>
     * 核心流程：
     * 1. 校验用户消息内容非空；若请求中未指定系统提示词，则使用配置的默认值
     * 2. 使用 Spring AI Advisor 按 user/assistant 角色读取并写入短期记忆
     * 3. 将请求中的模型参数（model/temperature/maxTokens）合并到 DashScopeChatOptions
     * 4. 调用底层 ChatModel 获取模型响应，并由 Advisor 自动记录本轮 user/assistant 消息
     * 5. 将 Spring AI 的标准 ChatResponse 转换为自定义的 AiChatResponse，
     *    携带会话ID、回复内容、使用的模型及 Token 用量统计
     * </p>
     *
     * @param request 对话请求，包含消息内容、可选的系统提示词与模型参数
     * @return 标准化的 AI 对话回复，包含生成的文本、模型名及用量信息
     */
    public AiChatResponse chat(ChatRequest request)
    {
        String message = requireText(request == null ? null : request.getMessage(), "消息内容不能为空");
        String systemPrompt = defaultIfBlank(request.getSystemPrompt(), aiProxyProperties.getChat().getDefaultSystemPrompt());
        String conversationId = resolveConversationId(request.getConversationId());

        ChatResponse modelResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .options(buildOptions(request))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setContent(modelResponse.getResult().getOutput().getText());
        response.setModel(modelResponse.getMetadata().getModel());
        response.setUsage(toUsage(modelResponse.getMetadata().getUsage()));
        return response;
    }

    public String modelName()
    {
        return chatModel.getClass().getName();
    }

    /**
     * 根据请求参数构建 DashScope 调用选项。
     * <p>
     * 仅当调用方显式指定了 model / temperature / maxTokens 时才覆盖默认值，
     * 未指定的字段会回落到 Spring AI DashScope 自动配置的默认行为。
     * </p>
     *
     * @param request 原始请求，可能部分字段为 null
     * @return 合并后的 DashScopeChatOptions 实例
     */
    private DashScopeChatOptions buildOptions(ChatRequest request)
    {
        DashScopeChatOptions options = new DashScopeChatOptions();
        if (StringUtils.isNotBlank(request.getModel()))
        {
            options.setModel(request.getModel());
        }
        if (request.getTemperature() != null)
        {
            options.setTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null)
        {
            options.setMaxTokens(request.getMaxTokens());
        }
        return options;
    }

    /**
     * 将 Spring AI 的标准 Usage 对象映射为内部响应结构。
     * 模型未返回用量信息时返回 null，由调用方自行处理。
     *
     * @param usage 底层模型的 Token 用量统计
     * @return 转换后的 Usage 对象；若入参为 null 则返回 null
     */
    private AiChatResponse.Usage toUsage(Usage usage)
    {
        if (usage == null)
        {
            return null;
        }
        AiChatResponse.Usage result = new AiChatResponse.Usage();
        result.setInputTokens(usage.getPromptTokens());
        result.setOutputTokens(usage.getCompletionTokens());
        result.setTotalTokens(usage.getTotalTokens());
        return result;
    }

    private String requireText(String value, String message)
    {
        if (StringUtils.isBlank(value))
        {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue)
    {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private String resolveConversationId(String conversationId)
    {
        return StringUtils.isBlank(conversationId) ? ChatMemory.DEFAULT_CONVERSATION_ID : conversationId;
    }
}
