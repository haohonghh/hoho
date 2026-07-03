package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MemorySummaryService.class);

    private static final String SUMMARY_PREFIX = "历史摘要：";

    private final ChatMemory chatMemory;

    private final AiProxyProperties aiProxyProperties;

    public MemorySummaryService(ChatMemory chatMemory, AiProxyProperties aiProxyProperties)
    {
        this.chatMemory = chatMemory;
        this.aiProxyProperties = aiProxyProperties;
    }

    public SummaryDebugInfo summarizeIfNecessary(String conversationId)
    {
        List<Message> messages = chatMemory.get(conversationId);
        SummaryDebugInfo debugInfo = new SummaryDebugInfo();
        debugInfo.setConversationId(conversationId);
        debugInfo.setOriginalMessageCount(messages == null ? 0 : messages.size());
        debugInfo.setKeepRecentMessages(Math.max(1, aiProxyProperties.getMemory().getSummaryKeepRecentMessages()));
        debugInfo.setTriggerThreshold(aiProxyProperties.getMemory().getSummaryTriggerMessageCount());
        if (messages == null || messages.size() <= aiProxyProperties.getMemory().getSummaryTriggerMessageCount())
        {
            debugInfo.setTriggered(false);
            debugInfo.setFinalMessageCount(messages == null ? 0 : messages.size());
            return debugInfo;
        }

        int keepRecentMessages = Math.max(1, aiProxyProperties.getMemory().getSummaryKeepRecentMessages());
        if (messages.size() <= keepRecentMessages)
        {
            debugInfo.setTriggered(false);
            debugInfo.setFinalMessageCount(messages.size());
            return debugInfo;
        }

        int splitIndex = Math.max(0, messages.size() - keepRecentMessages);
        List<Message> recentMessages = new ArrayList<>(messages.subList(splitIndex, messages.size()));
        String summary = buildSummary(messages.subList(0, splitIndex));
        if (StringUtils.isBlank(summary))
        {
            debugInfo.setTriggered(false);
            debugInfo.setFinalMessageCount(messages.size());
            return debugInfo;
        }

        List<Message> summarizedMessages = new ArrayList<>();
        summarizedMessages.add(new SystemMessage(summary));
        summarizedMessages.addAll(recentMessages);
        chatMemory.clear(conversationId);
        chatMemory.add(conversationId, summarizedMessages);
        debugInfo.setTriggered(true);
        debugInfo.setFinalMessageCount(summarizedMessages.size());
        debugInfo.setSummaryPreview(summary.length() > 120 ? summary.substring(0, 120) : summary);
        log.info("短期记忆摘要触发 conversationId={}, originalCount={}, finalCount={}, keepRecentMessages={}, threshold={}, summaryPreview={}",
                conversationId, debugInfo.getOriginalMessageCount(), debugInfo.getFinalMessageCount(),
                keepRecentMessages, debugInfo.getTriggerThreshold(), debugInfo.getSummaryPreview());
        return debugInfo;
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

    public static class SummaryDebugInfo
    {
        private String conversationId;

        private boolean triggered;

        private Integer originalMessageCount;

        private Integer finalMessageCount;

        private Integer keepRecentMessages;

        private Integer triggerThreshold;

        private String summaryPreview;

        public String getConversationId()
        {
            return conversationId;
        }

        public void setConversationId(String conversationId)
        {
            this.conversationId = conversationId;
        }

        public boolean isTriggered()
        {
            return triggered;
        }

        public void setTriggered(boolean triggered)
        {
            this.triggered = triggered;
        }

        public Integer getOriginalMessageCount()
        {
            return originalMessageCount;
        }

        public void setOriginalMessageCount(Integer originalMessageCount)
        {
            this.originalMessageCount = originalMessageCount;
        }

        public Integer getFinalMessageCount()
        {
            return finalMessageCount;
        }

        public void setFinalMessageCount(Integer finalMessageCount)
        {
            this.finalMessageCount = finalMessageCount;
        }

        public Integer getKeepRecentMessages()
        {
            return keepRecentMessages;
        }

        public void setKeepRecentMessages(Integer keepRecentMessages)
        {
            this.keepRecentMessages = keepRecentMessages;
        }

        public Integer getTriggerThreshold()
        {
            return triggerThreshold;
        }

        public void setTriggerThreshold(Integer triggerThreshold)
        {
            this.triggerThreshold = triggerThreshold;
        }

        public String getSummaryPreview()
        {
            return summaryPreview;
        }

        public void setSummaryPreview(String summaryPreview)
        {
            this.summaryPreview = summaryPreview;
        }
    }
}
