package com.hoho.kb.model.response;

import java.util.List;

/**
 * AI代理向量化响应
 *
 * @author hoho
 */
public class AiEmbeddingResponse
{
    private int dimension;

    private List<Double> vector;

    public int getDimension()
    {
        return dimension;
    }

    public void setDimension(int dimension)
    {
        this.dimension = dimension;
    }

    public List<Double> getVector()
    {
        return vector;
    }

    public void setVector(List<Double> vector)
    {
        this.vector = vector;
    }
}
