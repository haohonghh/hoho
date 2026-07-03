package com.hoho.bot.model.request;

/**
 * AI代理对话请求
 *
 * @author hoho
 */
public class AiChatRequest
{
    private String conversationId;

    private String systemPrompt;

    private String message;

    private String scene;

    private String agentCode;

    private String model;

    private Double temperature;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt)
    {
        this.systemPrompt = systemPrompt;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Double getTemperature()
    {
        return temperature;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public void setTemperature(Double temperature)
    {
        this.temperature = temperature;
    }
}
