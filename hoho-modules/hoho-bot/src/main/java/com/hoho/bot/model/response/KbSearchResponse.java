package com.hoho.bot.model.response;

import java.util.List;

/**
 * 知识库检索响应
 *
 * @author hoho
 */
public class KbSearchResponse
{
    private String query;

    private List<KbSearchItem> items;

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public List<KbSearchItem> getItems()
    {
        return items;
    }

    public void setItems(List<KbSearchItem> items)
    {
        this.items = items;
    }
}
