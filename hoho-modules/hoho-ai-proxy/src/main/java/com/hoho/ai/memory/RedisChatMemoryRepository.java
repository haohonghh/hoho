package com.hoho.ai.memory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.hoho.ai.config.AiProxyProperties;
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

    private final AiProxyProperties aiProxyProperties;

    public RedisChatMemoryRepository(RedisService redisService, AiProxyProperties aiProxyProperties)
    {
        this.redisService = redisService;
        this.aiProxyProperties = aiProxyProperties;
    }

    @Override
    public List<String> findConversationIds()
    {
        Collection<String> keys = redisService.keys(memoryKey("*"));
        if (keys == null)
        {
            return Collections.emptyList();
        }
        int prefixLength = aiProxyProperties.getMemory().getKeyPrefix().length();
        return keys.stream()
                .map(key -> key.substring(prefixLength))
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId)
    {
        List<AiMemoryMessage> cachedMessages = redisService.getCacheObject(memoryKey(conversationId));
        if (cachedMessages == null)
        {
            return Collections.emptyList();
        }
        return cachedMessages.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages)
    {
        List<AiMemoryMessage> cachedMessages = messages == null ? Collections.emptyList()
                : messages.stream().map(this::fromMessage).collect(Collectors.toList());
        redisService.setCacheObject(memoryKey(conversationId), cachedMessages,
                aiProxyProperties.getMemory().getTtlMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public void deleteByConversationId(String conversationId)
    {
        redisService.deleteObject(memoryKey(conversationId));
    }

    private String memoryKey(String conversationId)
    {
        return aiProxyProperties.getMemory().getKeyPrefix() + conversationId;
    }

    private AiMemoryMessage fromMessage(Message message)
    {
        return new AiMemoryMessage(message.getMessageType().getValue(), message.getText());
    }

    private Message toMessage(AiMemoryMessage cachedMessage)
    {
        MessageType messageType = MessageType.fromValue(cachedMessage.getType());
        if (MessageType.USER.equals(messageType))
        {
            return new UserMessage(cachedMessage.getText());
        }
        if (MessageType.ASSISTANT.equals(messageType))
        {
            return new AssistantMessage(cachedMessage.getText());
        }
        return new SystemMessage(cachedMessage.getText());
    }
}
