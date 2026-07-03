package com.hoho.ai.service;

import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.MemoryAppendRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryServiceTest
{
    @Test
    void 追加一轮问答时保留用户和助手角色()
    {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        MemoryService memoryService = new MemoryService(chatMemory, new MemorySummaryService(chatMemory, new AiProxyProperties()));

        MemoryAppendRequest request = new MemoryAppendRequest();
        request.setConversationId("session-1");
        request.setUserMessage("电脑无法联网怎么办？");
        request.setAssistantMessage("请先检查网线和 Wi-Fi。");
        memoryService.append(request);

        List<Message> messages = chatMemory.get("session-1");
        assertEquals(MessageType.USER, messages.get(0).getMessageType());
        assertEquals("电脑无法联网怎么办？", messages.get(0).getText());
        assertEquals(MessageType.ASSISTANT, messages.get(1).getMessageType());
        assertEquals("请先检查网线和 Wi-Fi。", messages.get(1).getText());
    }
}
