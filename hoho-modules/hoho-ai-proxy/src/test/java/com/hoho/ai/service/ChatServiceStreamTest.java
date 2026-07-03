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
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceStreamTest
{
    @Test
    void 流式对话时按顺序输出内容片段并保留记忆上下文()
    {
        StreamingRecordingChatModel chatModel = new StreamingRecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        chatMemory.add("session-stream", List.of(new UserMessage("上一轮问题"), new AssistantMessage("上一轮回答")));
        ChatService chatService = new ChatService(chatModel, chatMemory, new AiProxyProperties());

        ChatRequest request = new ChatRequest();
        request.setConversationId("session-stream");
        request.setSystemPrompt("当前系统提示词");
        request.setMessage("请流式回答");

        List<String> chunks = chatService.stream(request).collectList().block();

        assertEquals(List.of("先检查网络", "，再检查 DNS"), chunks);
        List<Message> messages = chatModel.lastStreamPrompt.getInstructions();
        assertEquals(MessageType.USER, messages.get(0).getMessageType());
        assertEquals("上一轮问题", messages.get(0).getText());
        assertEquals(MessageType.ASSISTANT, messages.get(1).getMessageType());
        assertEquals("上一轮回答", messages.get(1).getText());
        assertEquals(MessageType.SYSTEM, messages.get(2).getMessageType());
        assertEquals("当前系统提示词", messages.get(2).getText());
        assertEquals(MessageType.USER, messages.get(3).getMessageType());
        assertEquals("请流式回答", messages.get(3).getText());
    }

    private static class StreamingRecordingChatModel implements ChatModel
    {
        private Prompt lastStreamPrompt;

        @Override
        public ChatResponse call(Prompt prompt)
        {
            return new ChatResponse(
                    List.of(new Generation(new AssistantMessage("同步回答"))),
                    ChatResponseMetadata.builder().model("test-model").build());
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt)
        {
            lastStreamPrompt = prompt;
            return Flux.just("先检查网络", "，再检查 DNS")
                    .map(chunk -> new ChatResponse(
                            List.of(new Generation(new AssistantMessage(chunk))),
                            ChatResponseMetadata.builder().model("test-model").build()));
        }

        @Override
        public ChatOptions getDefaultOptions()
        {
            return null;
        }
    }
}
