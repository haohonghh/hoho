package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.ChatRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void 调用对话时通过SpringAi日志Advisor输出请求和响应()
    {
        RecordingChatModel chatModel = new RecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        ChatService chatService = new ChatService(chatModel, chatMemory, new AiProxyProperties());
        Logger logger = (Logger) LoggerFactory.getLogger(SimpleLoggerAdvisor.class);
        Level oldLevel = logger.getLevel();
        RecordingAppender appender = new RecordingAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try
        {
            ChatRequest request = new ChatRequest();
            request.setConversationId("session-1");
            request.setSystemPrompt("当前系统提示词");
            request.setMessage("当前用户问题");
            chatService.chat(request);
        }
        finally
        {
            logger.detachAppender(appender);
            logger.setLevel(oldLevel);
        }

        assertTrue(appender.messages.stream().anyMatch(message -> message.contains("request:")),
                "应输出 Spring AI 请求日志");
        assertTrue(appender.messages.stream().anyMatch(message -> message.contains("response:")),
                "应输出 Spring AI 响应日志");
    }

    @Test
    void 未指定模型时自动使用默认模型()
    {
        RecordingChatModel chatModel = new RecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        AiProxyProperties properties = new AiProxyProperties();
        properties.getChat().setDefaultModel("qwen-turbo");
        properties.getChat().setAvailableModels(List.of("qwen-plus", "qwen-turbo"));
        ChatService chatService = new ChatService(chatModel, chatMemory, properties);

        ChatRequest request = new ChatRequest();
        request.setConversationId("session-1");
        request.setSystemPrompt("当前系统提示词");
        request.setMessage("当前用户问题");

        chatService.chat(request);

        assertEquals("qwen-turbo", ((com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions) chatModel.lastPrompt.getOptions()).getModel());
    }

    @Test
    void 指定了白名单外模型时拒绝调用()
    {
        RecordingChatModel chatModel = new RecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        AiProxyProperties properties = new AiProxyProperties();
        properties.getChat().setDefaultModel("qwen-plus");
        properties.getChat().setAvailableModels(List.of("qwen-plus", "qwen-turbo"));
        ChatService chatService = new ChatService(chatModel, chatMemory, properties);

        ChatRequest request = new ChatRequest();
        request.setConversationId("session-1");
        request.setSystemPrompt("当前系统提示词");
        request.setMessage("当前用户问题");
        request.setModel("gpt-4");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> chatService.chat(request));

        assertEquals("模型不在可用列表中", exception.getMessage());
    }

    @Test
    void 指定场景时优先使用场景绑定模型()
    {
        RecordingChatModel chatModel = new RecordingChatModel();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        AiProxyProperties properties = new AiProxyProperties();
        properties.getChat().setDefaultModel("qwen-plus");
        properties.getChat().setAvailableModels(List.of("qwen-plus", "qwen-turbo"));
        AiProxyProperties.ChatScene scene = new AiProxyProperties.ChatScene();
        scene.setModel("qwen-turbo");
        properties.getChat().getScenes().put("chat_general", scene);
        ChatService chatService = new ChatService(chatModel, chatMemory, properties);

        ChatRequest request = new ChatRequest();
        request.setConversationId("session-1");
        request.setSystemPrompt("当前系统提示词");
        request.setMessage("当前用户问题");
        request.setScene("chat_general");

        chatService.chat(request);

        assertEquals("qwen-turbo", ((com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions) chatModel.lastPrompt.getOptions()).getModel());
    }

    private static class RecordingAppender extends AppenderBase<ILoggingEvent>
    {
        private final List<String> messages = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject)
        {
            messages.add(eventObject.getFormattedMessage());
        }
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
