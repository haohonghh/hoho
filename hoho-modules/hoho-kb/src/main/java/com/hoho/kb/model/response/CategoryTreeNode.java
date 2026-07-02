package com.hoho.kb.model.response;

import java.util.ArrayList;
import java.util.List;

import com.hoho.kb.domain.Category;

/**
 * 分类树节点
 *
 * @author hoho
 */
public class CategoryTreeNode extends Category
{
    private List<CategoryTreeNode> children = new ArrayList<>();

    public List<CategoryTreeNode> getChildren()
    {
        return children;
    }

    public void setChildren(List<CategoryTreeNode> children)
    {
        this.children = children;
    }
}
