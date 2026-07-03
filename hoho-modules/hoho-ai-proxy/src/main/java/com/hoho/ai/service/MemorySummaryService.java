package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

/**
 * 记忆摘要服务
 *
 * @author hoho
 */
@Service
public class MemorySummaryService
{
    private static final String SUMMARY_PREFIX = "历史摘要：";

    private final ChatMemory chatMemory;

    private final AiProxyProperties aiProxyProperties;

    public MemorySummaryService(ChatMemory chatMemory, AiProxyProperties aiProxyProperties)
    {
        this.chatMemory = chatMemory;
        this.aiProxyProperties = aiProxyProperties;
    }

    public void summarizeIfNecessary(String conversationId)
    {
        List<Message> messages = chatMemory.get(conversationId);
        if (messages == null || messages.size() <= aiProxyProperties.getMemory().getSummaryTriggerMessageCount())
        {
            return;
        }

        int keepRecentMessages = Math.max(1, aiProxyProperties.getMemory().getSummaryKeepRecentMessages());
        if (messages.size() <= keepRecentMessages)
        {
            return;
        }

        int splitIndex = Math.max(0, messages.size() - keepRecentMessages);
        List<Message> recentMessages = new ArrayList<>(messages.subList(splitIndex, messages.size()));
        String summary = buildSummary(messages.subList(0, splitIndex));
        if (StringUtils.isBlank(summary))
        {
            return;
        }

        List<Message> summarizedMessages = new ArrayList<>();
        summarizedMessages.add(new SystemMessage(summary));
        summarizedMessages.addAll(recentMessages);
        chatMemory.clear(conversationId);
        chatMemory.add(conversationId, summarizedMessages);
    }

    private String buildSummary(List<Message> messages)
    {
        StringBuilder builder = new StringBuilder(SUMMARY_PREFIX);
        for (Message message : messages)
        {
            if (message == null || StringUtils.isBlank(message.getText()))
            {
                continue;
            }
            if (MessageType.SYSTEM.equals(message.getMessageType()) && message.getText().startsWith(SUMMARY_PREFIX))
            {
                builder.append(message.getText().substring(SUMMARY_PREFIX.length()));
            }
            else if (MessageType.USER.equals(message.getMessageType()))
            {
                builder.append("用户：").append(message.getText().trim()).append('；');
            }
            else if (MessageType.ASSISTANT.equals(message.getMessageType()))
            {
                builder.append("助手：").append(message.getText().trim()).append('；');
            }
        }
        return builder.length() == SUMMARY_PREFIX.length() ? null : builder.toString();
    }
}
