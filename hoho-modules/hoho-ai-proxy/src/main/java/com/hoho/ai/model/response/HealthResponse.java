package com.hoho.ai.model.response;

/**
 * AI代理健康状态
 *
 * @author hoho
 */
public class HealthResponse
{
    private String status;

    private String chatModel;

    private String embeddingModel;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getChatModel()
    {
        return chatModel;
    }

    public void setChatModel(String chatModel)
    {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel()
    {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel)
    {
        this.embeddingModel = embeddingModel;
    }
}
