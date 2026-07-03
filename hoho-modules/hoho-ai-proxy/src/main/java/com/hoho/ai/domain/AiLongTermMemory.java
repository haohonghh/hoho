package com.hoho.ai.domain;

import java.time.LocalDateTime;

/**
 * 用户长期记忆
 *
 * @author hoho
 */
public class AiLongTermMemory
{
    private Long id;

    private Long userId;

    private String conversationId;

    private String memoryType;

    private String memoryKey;

    private String memoryValue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getMemoryType()
    {
        return memoryType;
    }

    public void setMemoryType(String memoryType)
    {
        this.memoryType = memoryType;
    }

    public String getMemoryKey()
    {
        return memoryKey;
    }

    public void setMemoryKey(String memoryKey)
    {
        this.memoryKey = memoryKey;
    }

    public String getMemoryValue()
    {
        return memoryValue;
    }

    public void setMemoryValue(String memoryValue)
    {
        this.memoryValue = memoryValue;
    }

    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime)
    {
        this.updateTime = updateTime;
    }
}
