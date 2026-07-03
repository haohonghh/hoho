package com.hoho.ai.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hoho.ai.domain.AiLongTermMemory;
import com.hoho.ai.mapper.AiLongTermMemoryMapper;
import com.hoho.ai.model.request.LongTermMemoryQueryRequest;
import com.hoho.ai.model.request.LongTermMemoryUpsertRequest;
import com.hoho.ai.model.response.LongTermMemoryProfileResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LongTermMemoryServiceTest
{
    @Test
    void 已存在同类型记忆时执行覆盖更新()
    {
        InMemoryAiLongTermMemoryMapper mapper = new InMemoryAiLongTermMemoryMapper();
        LongTermMemoryService service = new LongTermMemoryService(mapper);

        LongTermMemoryUpsertRequest first = new LongTermMemoryUpsertRequest();
        first.setUserId(1001L);
        first.setConversationId("session-1");
        first.setMemoryType("preference");
        first.setMemoryKey("network");
        first.setMemoryValue("偏好图文步骤");
        service.upsert(first);

        LongTermMemoryUpsertRequest second = new LongTermMemoryUpsertRequest();
        second.setUserId(1001L);
        second.setConversationId("session-2");
        second.setMemoryType("preference");
        second.setMemoryKey("network");
        second.setMemoryValue("偏好简洁步骤");
        AiLongTermMemory saved = service.upsert(second);

        assertNotNull(saved.getId());
        assertEquals("偏好简洁步骤", saved.getMemoryValue());
        assertEquals("session-2", saved.getConversationId());
        assertEquals(1, mapper.storage.size());
    }

    @Test
    void 按用户查询时只返回对应用户记忆()
    {
        InMemoryAiLongTermMemoryMapper mapper = new InMemoryAiLongTermMemoryMapper();
        LongTermMemoryService service = new LongTermMemoryService(mapper);

        LongTermMemoryUpsertRequest first = new LongTermMemoryUpsertRequest();
        first.setUserId(1001L);
        first.setConversationId("session-1");
        first.setMemoryType("fact");
        first.setMemoryKey("department");
        first.setMemoryValue("网络运维部");
        service.upsert(first);

        LongTermMemoryUpsertRequest second = new LongTermMemoryUpsertRequest();
        second.setUserId(1002L);
        second.setConversationId("session-2");
        second.setMemoryType("fact");
        second.setMemoryKey("department");
        second.setMemoryValue("桌面支持部");
        service.upsert(second);

        LongTermMemoryQueryRequest query = new LongTermMemoryQueryRequest();
        query.setUserId(1001L);
        List<AiLongTermMemory> result = service.query(query);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getUserId());
        assertEquals("网络运维部", result.get(0).getMemoryValue());
    }

    @Test
    void 可按用户聚合返回偏好身份与环境画像()
    {
        InMemoryAiLongTermMemoryMapper mapper = new InMemoryAiLongTermMemoryMapper();
        LongTermMemoryService service = new LongTermMemoryService(mapper);

        service.upsert(memory(1001L, "session-1", "preference", "reply_language", "中文"));
        service.upsert(memory(1001L, "session-1", "profile", "role", "运维"));
        service.upsert(memory(1001L, "session-1", "environment", "os", "Windows"));
        service.upsert(memory(1001L, "session-1", "environment", "network", "代理网络"));

        LongTermMemoryQueryRequest query = new LongTermMemoryQueryRequest();
        query.setUserId(1001L);
        LongTermMemoryProfileResponse response = service.profile(query);

        assertEquals(1001L, response.getUserId());
        assertEquals("中文", response.getPreferences().get("reply_language"));
        assertEquals("运维", response.getProfile().get("role"));
        assertEquals("Windows", response.getEnvironment().get("os"));
        assertEquals("代理网络", response.getEnvironment().get("network"));
    }

    private LongTermMemoryUpsertRequest memory(Long userId, String conversationId, String memoryType, String memoryKey, String memoryValue)
    {
        LongTermMemoryUpsertRequest request = new LongTermMemoryUpsertRequest();
        request.setUserId(userId);
        request.setConversationId(conversationId);
        request.setMemoryType(memoryType);
        request.setMemoryKey(memoryKey);
        request.setMemoryValue(memoryValue);
        return request;
    }

    private static class InMemoryAiLongTermMemoryMapper implements AiLongTermMemoryMapper
    {
        private long nextId = 1L;

        private final List<AiLongTermMemory> storage = new ArrayList<>();

        @Override
        public AiLongTermMemory selectByUniqueKey(Long userId, String memoryType, String memoryKey)
        {
            return storage.stream()
                    .filter(item -> item.getUserId().equals(userId))
                    .filter(item -> item.getMemoryType().equals(memoryType))
                    .filter(item -> item.getMemoryKey().equals(memoryKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int insert(AiLongTermMemory memory)
        {
            memory.setId(nextId++);
            memory.setCreateTime(LocalDateTime.now());
            memory.setUpdateTime(LocalDateTime.now());
            storage.add(memory);
            return 1;
        }

        @Override
        public int update(AiLongTermMemory memory)
        {
            memory.setUpdateTime(LocalDateTime.now());
            return 1;
        }

        @Override
        public List<AiLongTermMemory> selectByUserId(Long userId)
        {
            return storage.stream()
                    .filter(item -> item.getUserId().equals(userId))
                    .toList();
        }
    }
}
