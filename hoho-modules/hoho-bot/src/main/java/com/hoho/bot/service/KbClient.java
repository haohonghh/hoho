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

    /**
     * 调用知识库服务执行混合检索（通常指向量检索 + 关键词检索的融合）。
     *
     * <p>底层通过 {@code POST /kb/search/hybrid} 向 kb 服务发送请求，
     * {@code query} 是用户原始消息文本，{@code topK} 控制返回的参考条目数量。
     *
     * <p>响应同样使用 {@code R<T>} 信封结构。当响应为 null 或业务状态为 error 时，
     * 抛出 {@link IllegalStateException}，由上层逻辑决定是否降级（如返回空结果列表）。
     *
     * <p>说明：本方法的调用方 BotService 已经对响应做了空安全兜底处理，
     * 即使本方法抛出异常或返回 empty items，BotService 仍会走后续的 AI 兜底流程。
     *
     * @param query 用户待检索的原始消息文本
     * @param topK  返回的最大条目数，调用方保证至少为 1
     * @return KbSearchResponse 包含原始 query 以及检索命中的 items 列表
     *         （可能为空）；失败时抛出异常
     * @throws IllegalStateException 当响应为 null 或业务状态为 error 时
     */
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
