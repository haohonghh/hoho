package com.hoho.ai.model.response;

import java.util.List;

/**
 * 批量向量化响应
 *
 * @author hoho
 */
public class BatchEmbeddingResponse
{
    private int dimension;

    private List<Item> items;

    public int getDimension()
    {
        return dimension;
    }

    public void setDimension(int dimension)
    {
        this.dimension = dimension;
    }

    public List<Item> getItems()
    {
        return items;
    }

    public void setItems(List<Item> items)
    {
        this.items = items;
    }

    public static class Item
    {
        private int index;

        private List<Double> vector;

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index = index;
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
}
