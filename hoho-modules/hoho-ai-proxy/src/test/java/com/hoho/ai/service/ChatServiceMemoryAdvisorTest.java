package com.hoho.ai.service;

import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceMemoryAdvisorTest
{
    @Test
    void 调用对话时通过Advisor按角色注入短期记忆()
    {
        RecordingChatModel chatModel = new RecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        chatMemory.add("session-1", List.of(new UserMessage("上一轮用户问题"), new AssistantMessage("上一轮助手回答")));
        ChatService chatService = new ChatService(chatModel, chatMemory, new AiProxyProperties());

        ChatRequest request = new ChatRequest();
        request.setConversationId("session-1");
        request.setSystemPrompt("当前系统提示词");
        request.setMessage("当前用户问题");

        chatService.chat(request);

        List<Message> messages = chatModel.lastPrompt.getInstructions();
        assertEquals(MessageType.USER, messages.get(0).getMessageType());
        assertEquals("上一轮用户问题", messages.get(0).getText());
        assertEquals(MessageType.ASSISTANT, messages.get(1).getMessageType());
        assertEquals("上一轮助手回答", messages.get(1).getText());
        assertEquals(MessageType.SYSTEM, messages.get(2).getMessageType());
        assertEquals("当前系统提示词", messages.get(2).getText());
        assertEquals(MessageType.USER, messages.get(3).getMessageType());
        assertEquals("当前用户问题", messages.get(3).getText());
    }

    private static class RecordingChatModel implements ChatModel
    {
        private Prompt lastPrompt;

        @Override
        public ChatResponse call(Prompt prompt)
        {
            lastPrompt = prompt;
            return new ChatResponse(
                    List.of(new Generation(new AssistantMessage("当前助手回答"))),
                    ChatResponseMetadata.builder().model("test-model").build());
        }

        @Override
        public ChatOptions getDefaultOptions()
        {
            return null;
        }
    }
}
