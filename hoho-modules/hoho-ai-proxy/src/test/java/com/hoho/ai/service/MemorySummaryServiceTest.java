package com.hoho.ai.service;

import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemorySummaryServiceTest
{
    @Test
    void 超过阈值时压缩旧消息为系统摘要并保留最近消息()
    {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatMemory.add("session-1", List.of(
                new UserMessage("用户问题1"),
                new AssistantMessage("助手回答1"),
                new UserMessage("用户问题2"),
                new AssistantMessage("助手回答2"),
                new UserMessage("用户问题3"),
                new AssistantMessage("助手回答3")));

        AiProxyProperties properties = new AiProxyProperties();
        properties.getMemory().setSummaryTriggerMessageCount(4);
        properties.getMemory().setSummaryKeepRecentMessages(2);
        MemorySummaryService summaryService = new MemorySummaryService(chatMemory, properties);

        summaryService.summarizeIfNecessary("session-1");

        List<Message> messages = chatMemory.get("session-1");
        assertEquals(3, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertTrue(messages.get(0).getText().contains("用户问题1"));
        assertTrue(messages.get(0).getText().contains("助手回答2"));
        assertEquals("用户问题3", messages.get(1).getText());
        assertEquals("助手回答3", messages.get(2).getText());
    }

    @Test
    void 未超过阈值时保持原始消息不变()
    {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatMemory.add("session-1", List.of(
                new UserMessage("用户问题1"),
                new AssistantMessage("助手回答1")));

        AiProxyProperties properties = new AiProxyProperties();
        properties.getMemory().setSummaryTriggerMessageCount(4);
        properties.getMemory().setSummaryKeepRecentMessages(2);
        MemorySummaryService summaryService = new MemorySummaryService(chatMemory, properties);

        summaryService.summarizeIfNecessary("session-1");

        List<Message> messages = chatMemory.get("session-1");
        assertEquals(2, messages.size());
        assertEquals("用户问题1", messages.get(0).getText());
        assertEquals("助手回答1", messages.get(1).getText());
    }

    @Test
    void 可返回摘要调试信息用于判断本轮是否触发压缩()
    {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatMemory.add("session-debug-2", List.of(
                new UserMessage("用户问题1"),
                new AssistantMessage("助手回答1"),
                new UserMessage("用户问题2"),
                new AssistantMessage("助手回答2"),
                new UserMessage("用户问题3"),
                new AssistantMessage("助手回答3")));

        AiProxyProperties properties = new AiProxyProperties();
        properties.getMemory().setSummaryTriggerMessageCount(4);
        properties.getMemory().setSummaryKeepRecentMessages(2);
        MemorySummaryService summaryService = new MemorySummaryService(chatMemory, properties);

        MemorySummaryService.SummaryDebugInfo debugInfo = summaryService.summarizeIfNecessary("session-debug-2");

        assertTrue(debugInfo.isTriggered());
        assertEquals(6, debugInfo.getOriginalMessageCount());
        assertEquals(3, debugInfo.getFinalMessageCount());
        assertEquals(2, debugInfo.getKeepRecentMessages());
        assertFalse(debugInfo.getSummaryPreview().isBlank());
    }
}
