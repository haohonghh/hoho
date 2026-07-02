package com.hoho.bot.model.request;

/**
 * 文本对话请求
 *
 * @author hoho
 */
public class BotChatRequest
{
    private String conversationId;

    private String message;

    private Integer topK;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Integer getTopK()
    {
        return topK;
    }

    public void setTopK(Integer topK)
    {
        this.topK = topK;
    }
}
