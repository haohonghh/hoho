package com.hoho.bot.service;

import java.util.HashMap;
import java.util.Map;

import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 知识库客户端
 *
 * @author hoho
 */
@Component
public class KbClient
{
    private final RestTemplate restTemplate;

    private final BotProperties botProperties;

    public KbClient(RestTemplate restTemplate, BotProperties botProperties)
    {
        this.restTemplate = restTemplate;
        this.botProperties = botProperties;
    }

    public KbSearchResponse hybridSearch(String query, int topK)
    {
        Map<String, Object> request = new HashMap<>();
        request.put("query", query);
        request.put("topK", topK);

        ResponseEntity<R<KbSearchResponse>> response = restTemplate.exchange(
                botProperties.getKb().getBaseUrl() + "/kb/search/hybrid",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<R<KbSearchResponse>>()
                {
                });

        R<KbSearchResponse> body = response.getBody();
        if (body == null || R.isError(body))
        {
            throw new IllegalStateException(body == null ? "知识库检索无响应" : body.getMsg());
        }
        return body.getData();
    }
}
