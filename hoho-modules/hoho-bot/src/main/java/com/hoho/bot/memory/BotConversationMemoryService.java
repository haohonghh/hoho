package com.hoho.bot.memory;

import java.util.List;

import com.hoho.bot.config.BotProperties;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * 机器人短期记忆服务
 *
 * @author hoho
 */
@Service
public class BotConversationMemoryService
{
    private final ChatMemory chatMemory;

    private final BotProperties botProperties;

    public BotConversationMemoryService(ChatMemory chatMemory, BotProperties botProperties)
    {
        this.chatMemory = chatMemory;
        this.botProperties = botProperties;
    }

    /**
     * 构建可注入到系统提示词中的短期记忆文本。
     *
     * @param conversationId 会话编号
     * @return 短期记忆文本；未开启或无历史时返回空字符串
     */
    public String buildMemoryPrompt(String conversationId)
    {
        if (!botProperties.getMemory().isEnabled() || StringUtils.isBlank(conversationId))
        {
            return "";
        }
        List<Message> messages = chatMemory.get(conversationId);
        if (messages == null || messages.isEmpty())
        {
            return "";
        }
        StringBuilder prompt = new StringBuilder("短期对话记忆：");
        for (Message message : messages)
        {
            if (message == null || StringUtils.isBlank(message.getText()))
            {
                continue;
            }
            prompt.append('\n').append(roleName(message)).append('：').append(message.getText());
        }
        return prompt.length() == "短期对话记忆：".length() ? "" : prompt.toString();
    }

    /**
     * 记录用户消息到Spring AI短期记忆。
     *
     * @param conversationId 会话编号
     * @param message        用户消息
     */
    public void recordUserMessage(String conversationId, String message)
    {
        if (shouldSkip(conversationId, message))
        {
            return;
        }
        chatMemory.add(conversationId, new UserMessage(message));
    }

    /**
     * 记录助手消息到Spring AI短期记忆。
     *
     * @param conversationId 会话编号
     * @param message        助手回复
     */
    public void recordAssistantMessage(String conversationId, String message)
    {
        if (shouldSkip(conversationId, message))
        {
            return;
        }
        chatMemory.add(conversationId, new AssistantMessage(message));
    }

    private boolean shouldSkip(String conversationId, String message)
    {
        return !botProperties.getMemory().isEnabled()
                || StringUtils.isBlank(conversationId)
                || StringUtils.isBlank(message);
    }

    private String roleName(Message message)
    {
        if (MessageType.USER.equals(message.getMessageType()))
        {
            return "用户";
        }
        if (MessageType.ASSISTANT.equals(message.getMessageType()))
        {
            return "助手";
        }
        return "系统";
    }
}
