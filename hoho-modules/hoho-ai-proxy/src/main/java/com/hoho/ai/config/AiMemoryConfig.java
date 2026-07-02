package com.hoho.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI短期记忆配置
 *
 * @author hoho
 */
@Configuration
public class AiMemoryConfig
{
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, AiProxyProperties aiProxyProperties)
    {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(Math.max(2, aiProxyProperties.getMemory().getMaxMessages()))
                .build();
    }
}
