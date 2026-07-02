package com.hoho.bot.model.response;

/**
 * AI代理对话响应
 *
 * @author hoho
 */
public class AiChatResponse
{
    private String conversationId;

    private String content;

    private String model;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }
}
