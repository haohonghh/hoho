package com.hoho.ai.controller;

import com.hoho.ai.model.request.BatchEmbeddingRequest;
import com.hoho.ai.model.request.EmbeddingRequest;
import com.hoho.ai.model.response.BatchEmbeddingResponse;
import com.hoho.ai.model.response.EmbeddingResponse;
import com.hoho.ai.service.EmbeddingService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 向量化接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/ai/embedding")
public class EmbeddingController
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService)
    {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public R<EmbeddingResponse> embed(@RequestBody EmbeddingRequest request)
    {
        try
        {
            return R.ok(embeddingService.embed(request));
        }
        catch (Exception e)
        {
            log.error("调用向量化模型失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/batch")
    public R<BatchEmbeddingResponse> embedBatch(@RequestBody BatchEmbeddingRequest request)
    {
        try
        {
            return R.ok(embeddingService.embedBatch(request));
        }
        catch (Exception e)
        {
            log.error("批量调用向量化模型失败", e);
            return R.fail(e.getMessage());
        }
    }
}
