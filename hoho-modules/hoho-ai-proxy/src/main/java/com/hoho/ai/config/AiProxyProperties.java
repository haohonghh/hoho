package com.hoho.ai.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        private String defaultModel = "qwen-plus";

        private List<String> availableModels = new ArrayList<>(List.of("qwen-plus"));

        private Map<String, ChatScene> scenes = new LinkedHashMap<>();

        public String getDefaultSystemPrompt()
        {
            return defaultSystemPrompt;
        }

        public void setDefaultSystemPrompt(String defaultSystemPrompt)
        {
            this.defaultSystemPrompt = defaultSystemPrompt;
        }

        public String getDefaultModel()
        {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel)
        {
            this.defaultModel = defaultModel;
        }

        public List<String> getAvailableModels()
        {
            return availableModels;
        }

        public void setAvailableModels(List<String> availableModels)
        {
            this.availableModels = availableModels;
        }

        public Map<String, ChatScene> getScenes()
        {
            return scenes;
        }

        public void setScenes(Map<String, ChatScene> scenes)
        {
            this.scenes = scenes;
        }
    }

    public static class ChatScene
    {
        private String model;

        private Double temperature;

        private Integer maxTokens;

        public String getModel()
        {
            return model;
        }

        public void setModel(String model)
        {
            this.model = model;
        }

        public Double getTemperature()
        {
            return temperature;
        }

        public void setTemperature(Double temperature)
        {
            this.temperature = temperature;
        }

        public Integer getMaxTokens()
        {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens)
        {
            this.maxTokens = maxTokens;
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

        private int summaryTriggerMessageCount = 12;

        private int summaryKeepRecentMessages = 4;

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

        public int getSummaryTriggerMessageCount()
        {
            return summaryTriggerMessageCount;
        }

        public void setSummaryTriggerMessageCount(int summaryTriggerMessageCount)
        {
            this.summaryTriggerMessageCount = summaryTriggerMessageCount;
        }

        public int getSummaryKeepRecentMessages()
        {
            return summaryKeepRecentMessages;
        }

        public void setSummaryKeepRecentMessages(int summaryKeepRecentMessages)
        {
            this.summaryKeepRecentMessages = summaryKeepRecentMessages;
        }
    }
}
