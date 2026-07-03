package com.hoho.ai.service;

import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.MemoryDebugQueryRequest;
import com.hoho.ai.model.request.MemoryAppendRequest;
import com.hoho.ai.model.response.MemoryDebugResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void 调试查询接口返回当前会话的短期记忆内容()
    {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        MemoryService memoryService = new MemoryService(chatMemory, new MemorySummaryService(chatMemory, new AiProxyProperties()));

        MemoryAppendRequest appendRequest = new MemoryAppendRequest();
        appendRequest.setConversationId("session-debug-1");
        appendRequest.setUserMessage("我是运维。");
        appendRequest.setAssistantMessage("收到。");
        memoryService.append(appendRequest);

        MemoryDebugQueryRequest queryRequest = new MemoryDebugQueryRequest();
        queryRequest.setConversationId("session-debug-1");
        MemoryDebugResponse response = memoryService.debug(queryRequest);

        assertEquals("session-debug-1", response.getConversationId());
        assertEquals(2, response.getTotalMessages());
        assertEquals("USER", response.getMessages().get(0).getRole());
        assertEquals("我是运维。", response.getMessages().get(0).getContent());
        assertEquals("ASSISTANT", response.getMessages().get(1).getRole());
        assertTrue(response.getMessages().get(1).getContent().contains("收到"));
    }
}
