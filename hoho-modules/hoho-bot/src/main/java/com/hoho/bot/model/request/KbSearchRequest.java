package com.hoho.bot.model.request;

/**
 * 知识库检索请求
 *
 * @author hoho
 */
public class KbSearchRequest
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
