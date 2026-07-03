package com.hoho.ai.service;

import java.util.List;

import com.hoho.ai.domain.AiLongTermMemory;
import com.hoho.ai.mapper.AiLongTermMemoryMapper;
import com.hoho.ai.model.request.LongTermMemoryQueryRequest;
import com.hoho.ai.model.request.LongTermMemoryUpsertRequest;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 用户长期记忆服务
 *
 * @author hoho
 */
@Service
public class LongTermMemoryService
{
    private final AiLongTermMemoryMapper aiLongTermMemoryMapper;

    public LongTermMemoryService(AiLongTermMemoryMapper aiLongTermMemoryMapper)
    {
        this.aiLongTermMemoryMapper = aiLongTermMemoryMapper;
    }

    public AiLongTermMemory upsert(LongTermMemoryUpsertRequest request)
    {
        validateUpsert(request);
        AiLongTermMemory existing = aiLongTermMemoryMapper.selectByUniqueKey(request.getUserId(), request.getMemoryType(),
                request.getMemoryKey());
        if (existing == null)
        {
            AiLongTermMemory memory = buildMemory(new AiLongTermMemory(), request);
            aiLongTermMemoryMapper.insert(memory);
            return memory;
        }
        buildMemory(existing, request);
        aiLongTermMemoryMapper.update(existing);
        return existing;
    }

    public List<AiLongTermMemory> query(LongTermMemoryQueryRequest request)
    {
        if (request == null || request.getUserId() == null)
        {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        return aiLongTermMemoryMapper.selectByUserId(request.getUserId());
    }

    private void validateUpsert(LongTermMemoryUpsertRequest request)
    {
        if (request == null || request.getUserId() == null)
        {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        if (StringUtils.isBlank(request.getMemoryType()))
        {
            throw new IllegalArgumentException("记忆类型不能为空");
        }
        if (StringUtils.isBlank(request.getMemoryKey()))
        {
            throw new IllegalArgumentException("记忆键不能为空");
        }
        if (StringUtils.isBlank(request.getMemoryValue()))
        {
            throw new IllegalArgumentException("记忆内容不能为空");
        }
    }

    private AiLongTermMemory buildMemory(AiLongTermMemory memory, LongTermMemoryUpsertRequest request)
    {
        memory.setUserId(request.getUserId());
        memory.setConversationId(StringUtils.isBlank(request.getConversationId()) ? null : request.getConversationId().trim());
        memory.setMemoryType(request.getMemoryType().trim());
        memory.setMemoryKey(request.getMemoryKey().trim());
        memory.setMemoryValue(request.getMemoryValue().trim());
        return memory;
    }
}
