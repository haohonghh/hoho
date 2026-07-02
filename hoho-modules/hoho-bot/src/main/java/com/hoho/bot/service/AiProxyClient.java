package com.hoho.bot.service;

import java.util.HashMap;
import java.util.Map;

import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.common.core.domain.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * AI代理客户端
 *
 * @author hoho
 */
@Component
public class AiProxyClient
{
    private final RestTemplate restTemplate;

    private final BotProperties botProperties;

    public AiProxyClient(RestTemplate restTemplate, BotProperties botProperties)
    {
        this.restTemplate = restTemplate;
        this.botProperties = botProperties;
    }

    public AiChatResponse chat(String conversationId, String systemPrompt, String message)
    {
        Map<String, Object> request = new HashMap<>();
        request.put("conversationId", conversationId);
        request.put("systemPrompt", systemPrompt);
        request.put("message", message);
        request.put("temperature", 0.3D);

        ResponseEntity<R<AiChatResponse>> response = restTemplate.exchange(
                botProperties.getAiProxy().getBaseUrl() + "/ai/chat",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<R<AiChatResponse>>()
                {
                });

        R<AiChatResponse> body = response.getBody();
        if (body == null || R.isError(body))
        {
            throw new IllegalStateException(body == null ? "AI代理对话无响应" : body.getMsg());
        }
        return body.getData();
    }
}
