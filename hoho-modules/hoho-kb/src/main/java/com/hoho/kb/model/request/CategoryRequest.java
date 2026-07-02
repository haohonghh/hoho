package com.hoho.kb.model.request;

/**
 * 分类保存请求
 *
 * @author hoho
 */
public class CategoryRequest
{
    private Long parentId;

    private String name;

    private Integer sort;

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }
}
