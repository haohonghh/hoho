package com.hoho.ai.config;

import com.hoho.ai.constants.AiConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI代理配置
 *
 * @author hoho
 */
@Component
@ConfigurationProperties(prefix = "hoho.ai")
public class AiProxyProperties
{
    private final Chat chat = new Chat();

    private final Embedding embedding = new Embedding();

    private final Memory memory = new Memory();

    public Chat getChat()
    {
        return chat;
    }

    public Embedding getEmbedding()
    {
        return embedding;
    }

    public Memory getMemory()
    {
        return memory;
    }

    public static class Chat
    {
        private String defaultSystemPrompt = AiConstants.DEFAULT_SYSTEM_PROMPT;

        public String getDefaultSystemPrompt()
        {
            return defaultSystemPrompt;
        }

        public void setDefaultSystemPrompt(String defaultSystemPrompt)
        {
            this.defaultSystemPrompt = defaultSystemPrompt;
        }
    }

    public static class Embedding
    {
        private int dimension = AiConstants.DEFAULT_EMBEDDING_DIMENSION;

        private int batchSize = AiConstants.DEFAULT_BATCH_SIZE;

        public int getDimension()
        {
            return dimension;
        }

        public void setDimension(int dimension)
        {
            this.dimension = dimension;
        }

        public int getBatchSize()
        {
            return batchSize;
        }

        public void setBatchSize(int batchSize)
        {
            this.batchSize = batchSize;
        }
    }

    public static class Memory
    {
        private int maxMessages = 10;

        private long ttlMinutes = 120L;

        private String keyPrefix = "hoho:ai:memory:";

        public int getMaxMessages()
        {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages)
        {
            this.maxMessages = maxMessages;
        }

        public long getTtlMinutes()
        {
            return ttlMinutes;
        }

        public void setTtlMinutes(long ttlMinutes)
        {
            this.ttlMinutes = ttlMinutes;
        }

        public String getKeyPrefix()
        {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix)
        {
            this.keyPrefix = keyPrefix;
        }
    }
}
