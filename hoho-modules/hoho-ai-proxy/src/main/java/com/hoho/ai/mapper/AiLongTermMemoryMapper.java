package com.hoho.ai.mapper;

import java.util.List;

import com.hoho.ai.domain.AiLongTermMemory;
import org.apache.ibatis.annotations.Param;

/**
 * 用户长期记忆数据层
 *
 * @author hoho
 */
public interface AiLongTermMemoryMapper
{
    AiLongTermMemory selectByUniqueKey(@Param("userId") Long userId, @Param("memoryType") String memoryType,
            @Param("memoryKey") String memoryKey);

    int insert(AiLongTermMemory memory);

    int update(AiLongTermMemory memory);

    List<AiLongTermMemory> selectByUserId(@Param("userId") Long userId);
}
