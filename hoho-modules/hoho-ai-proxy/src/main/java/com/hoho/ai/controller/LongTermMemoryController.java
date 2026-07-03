package com.hoho.ai.controller;

import java.util.List;

import com.hoho.ai.domain.AiLongTermMemory;
import com.hoho.ai.model.request.LongTermMemoryQueryRequest;
import com.hoho.ai.model.request.LongTermMemoryUpsertRequest;
import com.hoho.ai.service.LongTermMemoryService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 长期记忆接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/ai/memory/long-term")
public class LongTermMemoryController
{
    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryController.class);

    private final LongTermMemoryService longTermMemoryService;

    public LongTermMemoryController(LongTermMemoryService longTermMemoryService)
    {
        this.longTermMemoryService = longTermMemoryService;
    }

    @PostMapping("/upsert")
    public R<AiLongTermMemory> upsert(@RequestBody LongTermMemoryUpsertRequest request)
    {
        try
        {
            return R.ok(longTermMemoryService.upsert(request));
        }
        catch (Exception e)
        {
            log.error("写入长期记忆失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/query")
    public R<List<AiLongTermMemory>> query(@RequestBody LongTermMemoryQueryRequest request)
    {
        try
        {
            return R.ok(longTermMemoryService.query(request));
        }
        catch (Exception e)
        {
            log.error("查询长期记忆失败", e);
            return R.fail(e.getMessage());
        }
    }
}
