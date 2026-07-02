package com.hoho.bot.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.hoho.bot.config.BotProperties;
import com.hoho.common.core.utils.StringUtils;
import com.hoho.common.redis.service.RedisService;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Repository;

/**
 * 基于Redis的Spring AI短期记忆仓储
 *
 * @author hoho
 */
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository
{
    private final RedisService redisService;

    private final BotProperties botProperties;

    public RedisChatMemoryRepository(RedisService redisService, BotProperties botProperties)
    {
        this.redisService = redisService;
        this.botProperties = botProperties;
    }

    @Override
    public List<String> findConversationIds()
    {
        Collection<String> keys = redisService.keys(memoryKey("*"));
        if (keys == null || keys.isEmpty())
        {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(key -> key.substring(botProperties.getMemory().getKeyPrefix().length()))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId)
    {
        List<BotMemoryMessage> cachedMessages = redisService.getCacheObject(memoryKey(conversationId));
        if (cachedMessages == null || cachedMessages.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Message> messages = new ArrayList<>();
        for (BotMemoryMessage cachedMessage : cachedMessages)
        {
            Message message = toMessage(cachedMessage);
            if (message != null)
            {
                messages.add(message);
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages)
    {
        List<BotMemoryMessage> cachedMessages = messages == null ? Collections.emptyList()
                : messages.stream().map(this::fromMessage).toList();
        redisService.setCacheObject(memoryKey(conversationId), cachedMessages,
                botProperties.getMemory().getTtlMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public void deleteByConversationId(String conversationId)
    {
        redisService.deleteObject(memoryKey(conversationId));
    }

    private String memoryKey(String conversationId)
    {
        return botProperties.getMemory().getKeyPrefix() + conversationId;
    }

    private BotMemoryMessage fromMessage(Message message)
    {
        return new BotMemoryMessage(message.getMessageType().getValue(), message.getText());
    }

    private Message toMessage(BotMemoryMessage cachedMessage)
    {
        if (cachedMessage == null || StringUtils.isBlank(cachedMessage.getText()))
        {
            return null;
        }
        MessageType messageType = MessageType.fromValue(cachedMessage.getType());
        if (MessageType.USER.equals(messageType))
        {
            return new UserMessage(cachedMessage.getText());
        }
        if (MessageType.ASSISTANT.equals(messageType))
        {
            return new AssistantMessage(cachedMessage.getText());
        }
        if (MessageType.SYSTEM.equals(messageType))
        {
            return new SystemMessage(cachedMessage.getText());
        }
        return null;
    }
}
