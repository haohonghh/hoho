package com.hoho.kb.model.response;

import java.util.List;

/**
 * 检索响应
 *
 * @author hoho
 */
public class SearchResponse
{
    private String query;

    private List<SearchItem> items;

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public List<SearchItem> getItems()
    {
        return items;
    }

    public void setItems(List<SearchItem> items)
    {
        this.items = items;
    }
}
