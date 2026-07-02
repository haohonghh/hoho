package com.hoho.bot.api;

import com.hoho.bot.api.factory.RemoteKbFallbackFactory;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
