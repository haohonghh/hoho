package com.hoho.bot.api;

import com.hoho.bot.api.factory.RemoteKbFallbackFactory;
import com.hoho.bot.model.request.KbQaRequest;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 知识库远程服务
 *
 * @author hoho
 */
@FeignClient(contextId = "remoteKbService", value = "hoho-kb",
        fallbackFactory = RemoteKbFallbackFactory.class)
public interface RemoteKbService
{
    /**
     * 混合检索知识库。
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/kb/search/hybrid")
    R<KbSearchResponse> hybridSearch(@RequestBody KbSearchRequest request);

    /**
     * 创建问答知识。
     *
     * @param request 问答知识请求
     * @return 问答知识编号
     */
    @PostMapping("/kb/qa")
    R<Long> createQa(@RequestBody KbQaRequest request);

    /**
     * 发布问答知识。
     *
     * @param id 问答知识编号
     * @return 发布结果
     */
    @PostMapping("/kb/qa/{id}/publish")
    R<Boolean> publishQa(@PathVariable("id") Long id);
}
