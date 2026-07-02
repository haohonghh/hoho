package com.hoho.kb.model.request;

/**
 * AI代理向量化请求
 *
 * @author hoho
 */
public class AiEmbeddingRequest
{
    private String text;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
