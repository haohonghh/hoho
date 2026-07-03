package com.hoho.bot.service;

import java.util.Objects;

import com.hoho.bot.model.request.AiChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * AI代理流式客户端
 *
 * @author hoho
 */
@Component
public class AiProxyStreamClient
{
    private static final Logger log = LoggerFactory.getLogger(AiProxyStreamClient.class);

    private final WebClient webClient;

    public AiProxyStreamClient(WebClient.Builder webClientBuilder)
    {
        this.webClient = webClientBuilder.baseUrl("http://hoho-ai-proxy").build();
    }

    public Flux<String> streamChat(String conversationId, String systemPrompt, String message)
    {
        AiChatRequest request = new AiChatRequest();
        request.setConversationId(conversationId);
        request.setSystemPrompt(systemPrompt);
        request.setMessage(message);
        request.setTemperature(0.3D);

        long start = System.currentTimeMillis();
        log.info("调用AI代理流式对话开始 conversationId={}, messageLength={}, systemPromptLength={}",
                conversationId, length(message), length(systemPrompt));
        return webClient.post()
                .uri("/ai/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(event -> "message".equals(event.event()))
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .doOnComplete(() -> log.info("调用AI代理流式对话完成 conversationId={}, cost={}ms",
                        conversationId, System.currentTimeMillis() - start));
    }

    private int length(String value)
    {
        return value == null ? 0 : value.length();
    }
}
