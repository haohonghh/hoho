package com.hoho.kb.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hoho.common.core.domain.R;
import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.config.KbProperties;
import com.hoho.kb.model.response.AiEmbeddingResponse;
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

    private final KbProperties kbProperties;

    public AiProxyClient(RestTemplate restTemplate, KbProperties kbProperties)
    {
        this.restTemplate = restTemplate;
        this.kbProperties = kbProperties;
    }

    /**
     * 调用远程AI代理服务将文本向量化。
     * <p>
     * 通过 RestTemplate 向 AI代理服务的 /ai/embedding 接口发送 POST 请求，
     * 将传入的文本转换为高维浮点数向量（稠密向量），用于后续的语义检索。
     * <p>
     * 响应格式：{"code": 200, "msg": "success", "data": {"vector": [...]}}
     *
     * @param text 需要向量化的原始文本
     * @return 向量表示（高维浮点数列表），维度由 AI模型决定
     * @throws IllegalArgumentException      当文本为空或空白时抛出
     * @throws IllegalStateException         当 AI代理服务无响应、返回错误码或数据为空时抛出
     */
    public List<Double> embedding(String text)
    {
        if (StringUtils.isBlank(text))
        {
            throw new IllegalArgumentException("向量化文本不能为空");
        }

        Map<String, String> request = new HashMap<>();
        request.put("text", text);

        ResponseEntity<R<AiEmbeddingResponse>> response = restTemplate.exchange(
                kbProperties.getAiProxy().getBaseUrl() + "/ai/embedding",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<R<AiEmbeddingResponse>>()
                {
                });

        R<AiEmbeddingResponse> body = response.getBody();
        if (body == null || R.isError(body) || body.getData() == null)
        {
            throw new IllegalStateException(body == null ? "AI代理向量化无响应" : body.getMsg());
        }
        return body.getData().getVector();
    }
}
