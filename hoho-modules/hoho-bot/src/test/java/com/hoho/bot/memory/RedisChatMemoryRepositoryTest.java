package com.hoho.bot.memory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.hoho.bot.config.BotProperties;
import com.hoho.common.redis.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisChatMemoryRepositoryTest
{
    @Test
    void 保存消息时写入Redis并设置过期时间()
    {
        BotProperties properties = new BotProperties();
        properties.getMemory().setTtlMinutes(30L);
        StubRedisService redisService = new StubRedisService();
        RedisChatMemoryRepository repository = new RedisChatMemoryRepository(redisService, properties);

        repository.saveAll("session-1", List.of(new UserMessage("第一句"), new AssistantMessage("第二句")));

        assertEquals("hoho:bot:memory:session-1", redisService.lastKey);
        assertEquals(30L, redisService.lastTimeout);
        assertEquals(TimeUnit.MINUTES, redisService.lastTimeUnit);

        List<Message> messages = repository.findByConversationId("session-1");
        assertEquals(2, messages.size());
        assertEquals("第一句", messages.get(0).getText());
        assertEquals("第二句", messages.get(1).getText());
    }

    private static class StubRedisService extends RedisService
    {
        private String lastKey;

        private Object lastValue;

        private Long lastTimeout;

        private TimeUnit lastTimeUnit;

        @Override
        public <T> void setCacheObject(String key, T value, Long timeout, TimeUnit timeUnit)
        {
            lastKey = key;
            lastValue = value;
            lastTimeout = timeout;
            lastTimeUnit = timeUnit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCacheObject(String key)
        {
            return (T) lastValue;
        }
    }
}
