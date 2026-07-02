package com.hoho.ai.model.request;

import java.util.List;

/**
 * 批量向量化请求
 *
 * @author hoho
 */
public class BatchEmbeddingRequest
{
    private List<String> texts;

    public List<String> getTexts()
    {
        return texts;
    }

    public void setTexts(List<String> texts)
    {
        this.texts = texts;
    }
}
