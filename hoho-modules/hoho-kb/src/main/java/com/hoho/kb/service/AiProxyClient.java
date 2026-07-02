package com.hoho.kb.service;

import java.util.List;

import com.hoho.common.core.domain.R;
import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.api.RemoteAiProxyService;
import com.hoho.kb.model.request.AiEmbeddingRequest;
import com.hoho.kb.model.response.AiEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI代理客户端
 *
 * @author hoho
 */
@Component
public class AiProxyClient
{
    private static final Logger log = LoggerFactory.getLogger(AiProxyClient.class);

    private final RemoteAiProxyService remoteAiProxyService;

    public AiProxyClient(RemoteAiProxyService remoteAiProxyService)
    {
        this.remoteAiProxyService = remoteAiProxyService;
    }

    /**
     * 调用远程AI代理服务将文本向量化。
     * <p>
     * 通过 Feign 向 AI代理服务的 /ai/embedding 接口发送 POST 请求，
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

        AiEmbeddingRequest request = new AiEmbeddingRequest();
        request.setText(text);

        long start = System.currentTimeMillis();
        log.info("调用AI代理向量化开始 textLength={}", text.length());
        R<AiEmbeddingResponse> response = remoteAiProxyService.embedding(request);
        if (response == null || R.isError(response) || response.getData() == null)
        {
            log.warn("调用AI代理向量化失败 textLength={}, cost={}ms, reason={}", text.length(),
                    System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "AI代理向量化无响应" : response.getMsg());
        }
        log.info("调用AI代理向量化完成 textLength={}, dimension={}, cost={}ms", text.length(),
                response.getData().getVector() == null ? 0 : response.getData().getVector().size(),
                System.currentTimeMillis() - start);
        return response.getData().getVector();
    }
}
