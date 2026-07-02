package com.hoho.kb.model.request;

/**
 * 检索请求
 *
 * @author hoho
 */
public class SearchRequest
{
    private String query;

    private Integer topK;

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
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
