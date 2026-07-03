package com.hoho.ai.controller;

import com.hoho.ai.model.request.MemoryAppendRequest;
import com.hoho.ai.model.request.MemoryDebugQueryRequest;
import com.hoho.ai.model.response.MemoryDebugResponse;
import com.hoho.ai.service.MemoryService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短期记忆接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/ai/memory")
public class MemoryController
{
    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService)
    {
        this.memoryService = memoryService;
    }

    @PostMapping("/append")
    public R<Void> append(@RequestBody MemoryAppendRequest request)
    {
        try
        {
            memoryService.append(request);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("追加短期记忆失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/debug")
    public R<MemoryDebugResponse> debug(@RequestBody MemoryDebugQueryRequest request)
    {
        try
        {
            return R.ok(memoryService.debug(request));
        }
        catch (Exception e)
        {
            log.error("查询短期记忆调试信息失败", e);
            return R.fail(e.getMessage());
        }
    }
}
