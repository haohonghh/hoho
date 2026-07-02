package com.hoho.ai.model.response;

import java.util.List;

/**
 * 单条向量化响应
 *
 * @author hoho
 */
public class EmbeddingResponse
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
