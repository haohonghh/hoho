package com.hoho.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.ChatRequest;
import com.hoho.ai.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerStreamTest
{
    @Test
    void 流式接口返回Sse事件流内容类型() throws Exception
    {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(new StubChatService())).build();
        ChatRequest request = new ChatRequest();
        request.setConversationId("session-stream");
        request.setMessage("请流式回答");

        mockMvc.perform(post("/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    private static class StubChatService extends ChatService
    {
        StubChatService()
        {
            super(new EmptyChatModel(), chatMemory(), new AiProxyProperties());
        }

        @Override
        public Flux<String> stream(ChatRequest request)
        {
            return Flux.just("第一段", "第二段");
        }

        private static ChatMemory chatMemory()
        {
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(10)
                    .build();
        }
    }

    private static class EmptyChatModel implements ChatModel
    {
        @Override
        public ChatResponse call(Prompt prompt)
        {
            return new ChatResponse(
                    java.util.List.of(new Generation(new AssistantMessage("同步回答"))),
                    ChatResponseMetadata.builder().model("test-model").build());
        }

        @Override
        public ChatOptions getDefaultOptions()
        {
            return null;
        }
    }
}
