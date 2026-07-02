package com.hoho.ai.model.request;

/**
 * 短期记忆追加请求
 *
 * @author hoho
 */
public class MemoryAppendRequest
{
    private String conversationId;

    private String userMessage;

    private String assistantMessage;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getUserMessage()
    {
        return userMessage;
    }

    public void setUserMessage(String userMessage)
    {
        this.userMessage = userMessage;
    }

    public String getAssistantMessage()
    {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage)
    {
        this.assistantMessage = assistantMessage;
    }
}
