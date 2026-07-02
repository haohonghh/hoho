package com.hoho.bot.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotMapperDialectTest
{
    @Test
    void 机器人Mapper使用PostgreSQL兼容时间函数() throws IOException
    {
        for (String mapper : List.of("mapper/bot/BotConversationMapper.xml", "mapper/bot/BotMessageMapper.xml"))
        {
            String content = new ClassPathResource(mapper).getContentAsString(StandardCharsets.UTF_8);

            assertTrue(content.contains("CURRENT_TIMESTAMP"));
            assertFalse(content.contains("sysdate()"));
        }
    }
}
