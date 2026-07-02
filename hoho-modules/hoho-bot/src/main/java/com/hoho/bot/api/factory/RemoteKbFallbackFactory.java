package com.hoho.bot.api.factory;

import com.hoho.bot.api.RemoteKbService;
import com.hoho.bot.model.request.KbQaRequest;
import com.hoho.bot.model.request.KbSearchRequest;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 知识库远程服务降级处理
 *
 * @author hoho
 */
@Component
public class RemoteKbFallbackFactory implements FallbackFactory<RemoteKbService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteKbFallbackFactory.class);

    @Override
    public RemoteKbService create(Throwable throwable)
    {
        log.error("知识库服务调用失败:{}", throwable.getMessage());
        return new RemoteKbService()
        {
            @Override
            public R<KbSearchResponse> hybridSearch(KbSearchRequest request)
            {
                return R.fail("知识库检索失败:" + throwable.getMessage());
            }

            @Override
            public R<Long> createQa(KbQaRequest request)
            {
                return R.fail("创建问答知识失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> publishQa(Long id)
            {
                return R.fail("发布问答知识失败:" + throwable.getMessage());
            }
        };
    }
}
