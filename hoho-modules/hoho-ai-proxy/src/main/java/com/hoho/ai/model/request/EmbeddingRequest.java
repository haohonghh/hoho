package com.hoho.ai.model.request;

/**
 * 单条向量化请求
 *
 * @author hoho
 */
public class EmbeddingRequest
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
