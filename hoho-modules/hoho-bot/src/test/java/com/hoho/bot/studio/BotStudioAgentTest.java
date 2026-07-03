package com.hoho.bot.studio;

import java.util.List;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.service.BotService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotStudioAgentTest
{
    @Test
    void Studio消息会调用现有BotService并返回可读调试内容()
    {
        StubBotService botService = new StubBotService();
        BotStudioAgent agent = new BotStudioAgent(botService);
        RunnableConfig config = RunnableConfig.builder().threadId("studio-session-1").build();

        List<NodeOutput> outputs = agent.stream(new UserMessage("电脑无法联网怎么办？"), config).collectList().block();

        assertEquals("studio-session-1", botService.lastRequest.getConversationId());
        assertEquals("电脑无法联网怎么办？", botService.lastRequest.getMessage());
        assertEquals(1, outputs.size());
        StreamingOutput<?> output = assertInstanceOf(StreamingOutput.class, outputs.get(0));
        AssistantMessage message = assertInstanceOf(AssistantMessage.class, output.message());
        assertTrue(message.getText().contains("请先检查网线和 Wi-Fi。"));
        assertTrue(message.getText().contains("来源：ai_with_kb"));
        assertTrue(message.getText().contains("参考资料："));
        assertTrue(message.getText().contains("电脑无法联网怎么办？"));
    }

    @Test
    void Loader只暴露Hoho机器人应用()
    {
        StubBotService botService = new StubBotService();
        BotStudioAgentLoader loader = new BotStudioAgentLoader(new BotStudioAgent(botService));

        assertEquals(List.of(BotStudioAgent.APP_NAME), loader.listAgents());
        assertSame(loader.loadAgent(BotStudioAgent.APP_NAME), loader.loadAgent(" hoho_bot "));
    }

    private static class StubBotService extends BotService
    {
        private BotChatRequest lastRequest;

        StubBotService()
        {
            super(null, null, null, new BotProperties(), null, null);
        }

        @Override
        public BotChatResponse chat(BotChatRequest request)
        {
            lastRequest = request;
            KbSearchItem reference = new KbSearchItem();
            reference.setQaId(1L);
            reference.setQuestion("电脑无法联网怎么办？");
            reference.setAnswer("检查网线和 Wi-Fi。");
            reference.setScore(0.62D);
            reference.setSource("hybrid");

            BotChatResponse response = new BotChatResponse();
            response.setConversationId(request.getConversationId());
            response.setAnswer("请先检查网线和 Wi-Fi。");
            response.setSource("ai_with_kb");
            response.setScore(0.62D);
            response.setReferences(List.of(reference));
            return response;
        }
    }
}
