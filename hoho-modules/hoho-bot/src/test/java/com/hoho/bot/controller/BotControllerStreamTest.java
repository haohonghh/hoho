package com.hoho.bot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.service.BotService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BotControllerStreamTest
{
    @Test
    void 流式接口返回事件流内容类型() throws Exception
    {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BotController(new StubBotService())).build();
        BotChatRequest request = new BotChatRequest();
        request.setConversationId("bot-stream-001");
        request.setMessage("电脑无法联网怎么办？");

        mockMvc.perform(post("/bot/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    private static class StubBotService extends BotService
    {
        StubBotService()
        {
            super(null, null, null, null, null, null);
        }

        @Override
        public Flux<String> streamChat(BotChatRequest request)
        {
            return Flux.just("第一段", "第二段");
        }
    }
}
