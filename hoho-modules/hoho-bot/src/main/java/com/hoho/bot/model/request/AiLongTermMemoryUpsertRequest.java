package com.hoho.bot.model.request;

/**
 * AI长期记忆写入请求
 *
 * @author hoho
 */
public class AiLongTermMemoryUpsertRequest
{
    private Long userId;

    private String conversationId;

    private String memoryType;

    private String memoryKey;

    private String memoryValue;

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
}
