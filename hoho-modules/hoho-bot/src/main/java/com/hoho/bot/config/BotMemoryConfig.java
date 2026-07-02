package com.hoho.bot.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI短期记忆配置
 *
 * @author hoho
 */
@Configuration
public class BotMemoryConfig
{
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, BotProperties botProperties)
    {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(Math.max(2, botProperties.getMemory().getMaxMessages()))
                .build();
    }
}
