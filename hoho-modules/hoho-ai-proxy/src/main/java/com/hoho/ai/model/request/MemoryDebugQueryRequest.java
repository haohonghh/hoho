package com.hoho.ai.model.request;

/**
 * 短期记忆调试查询请求
 *
 * @author hoho
 */
public class MemoryDebugQueryRequest
{
    private String conversationId;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }
}
