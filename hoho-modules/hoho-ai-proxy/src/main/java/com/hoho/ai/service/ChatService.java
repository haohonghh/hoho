package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.ChatRequest;
import com.hoho.ai.model.response.AiChatResponse;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
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

    private final AiProxyProperties aiProxyProperties;

    public ChatService(ChatModel chatModel, AiProxyProperties aiProxyProperties)
    {
        this.chatModel = chatModel;
        this.aiProxyProperties = aiProxyProperties;
    }

    public AiChatResponse chat(ChatRequest request)
    {
        String message = requireText(request == null ? null : request.getMessage(), "消息内容不能为空");
        String systemPrompt = defaultIfBlank(request.getSystemPrompt(), aiProxyProperties.getChat().getDefaultSystemPrompt());

        List<Message> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(systemPrompt))
        {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(message));

        Prompt prompt = new Prompt(messages, buildOptions(request));
        ChatResponse modelResponse = chatModel.call(prompt);

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(request.getConversationId());
        response.setContent(modelResponse.getResult().getOutput().getText());
        response.setModel(modelResponse.getMetadata().getModel());
        response.setUsage(toUsage(modelResponse.getMetadata().getUsage()));
        return response;
    }

    public String modelName()
    {
        return chatModel.getClass().getName();
    }

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
}
