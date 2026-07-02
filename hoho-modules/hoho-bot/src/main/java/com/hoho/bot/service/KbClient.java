package com.hoho.bot.service;

import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.model.request.KbQaRequest;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 知识库客户端
 *
 * @author hoho
 */
@Component
public class KbClient
{
    private static final Logger log = LoggerFactory.getLogger(KbClient.class);

    private final RemoteKbService remoteKbService;

    public KbClient(RemoteKbService remoteKbService)
    {
        this.remoteKbService = remoteKbService;
    }

    /**
     * 调用知识库服务执行混合检索（通常指向量检索 + 关键词检索的融合）。
     *
     * <p>底层通过 Feign 调用 {@code hoho-kb} 的 {@code POST /kb/search/hybrid} 接口，
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
        KbSearchRequest request = new KbSearchRequest();
        request.setQuery(query);
        request.setTopK(topK);

        long start = System.currentTimeMillis();
        log.info("调用知识库混合检索开始 queryLength={}, topK={}", length(query), topK);
        R<KbSearchResponse> response = remoteKbService.hybridSearch(request);
        if (response == null || R.isError(response))
        {
            log.warn("调用知识库混合检索失败 queryLength={}, topK={}, cost={}ms, reason={}", length(query), topK,
                    System.currentTimeMillis() - start, response == null ? "无响应" : response.getMsg());
            throw new IllegalStateException(response == null ? "知识库检索无响应" : response.getMsg());
        }
        log.info("调用知识库混合检索完成 queryLength={}, topK={}, itemCount={}, cost={}ms", length(query), topK,
                response.getData() == null || response.getData().getItems() == null ? 0 : response.getData().getItems().size(),
                System.currentTimeMillis() - start);
        return response.getData();
    }

    /**
     * 创建并发布问答知识。
     *
     * @param categoryId       分类编号
     * @param question         问题
     * @param answer           答案
     * @param similarQuestions 相似问法
     * @return 新建问答知识编号
     */
    public Long createAndPublishQa(Long categoryId, String question, String answer, String similarQuestions)
    {
        KbQaRequest request = new KbQaRequest();
        request.setCategoryId(categoryId);
        request.setQuestion(question);
        request.setAnswer(answer);
        request.setSimilarQuestions(similarQuestions);

        long start = System.currentTimeMillis();
        log.info("创建并发布问答知识开始 categoryId={}, questionLength={}, answerLength={}", categoryId,
                length(question), length(answer));
        R<Long> createResponse = remoteKbService.createQa(request);
        if (createResponse == null || R.isError(createResponse))
        {
            log.warn("创建问答知识失败 categoryId={}, cost={}ms, reason={}", categoryId,
                    System.currentTimeMillis() - start, createResponse == null ? "无响应" : createResponse.getMsg());
            throw new IllegalStateException(createResponse == null ? "创建问答知识无响应" : createResponse.getMsg());
        }
        Long qaId = createResponse.getData();
        R<Boolean> publishResponse = remoteKbService.publishQa(qaId);
        if (publishResponse == null || R.isError(publishResponse))
        {
            log.warn("发布问答知识失败 qaId={}, cost={}ms, reason={}", qaId, System.currentTimeMillis() - start,
                    publishResponse == null ? "无响应" : publishResponse.getMsg());
            throw new IllegalStateException(publishResponse == null ? "发布问答知识无响应" : publishResponse.getMsg());
        }
        log.info("创建并发布问答知识完成 qaId={}, categoryId={}, cost={}ms", qaId, categoryId,
                System.currentTimeMillis() - start);
        return qaId;
    }

    private int length(String value)
    {
        return value == null ? 0 : value.length();
    }
}
