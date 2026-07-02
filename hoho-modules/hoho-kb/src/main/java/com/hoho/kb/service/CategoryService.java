package com.hoho.kb.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.domain.Category;
import com.hoho.kb.mapper.CategoryMapper;
import com.hoho.kb.model.request.CategoryRequest;
import com.hoho.kb.model.response.CategoryTreeNode;
import org.springframework.stereotype.Service;

/**
 * 分类服务
 *
 * @author hoho
 */
@Service
public class CategoryService
{
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper)
    {
        this.categoryMapper = categoryMapper;
    }

    public Long create(CategoryRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getName()))
        {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        Category category = new Category();
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        categoryMapper.insert(category);
        return category.getId();
    }

    public List<CategoryTreeNode> tree()
    {
        List<Category> categories = categoryMapper.list();
        Map<Long, CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        for (Category category : categories)
        {
            nodeMap.put(category.getId(), copy(category));
        }

        List<CategoryTreeNode> roots = new ArrayList<>();
        for (CategoryTreeNode node : nodeMap.values())
        {
            CategoryTreeNode parent = nodeMap.get(node.getParentId());
            if (parent == null)
            {
                roots.add(node);
            }
            else
            {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private CategoryTreeNode copy(Category category)
    {
        CategoryTreeNode node = new CategoryTreeNode();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setSort(category.getSort());
        node.setStatus(category.getStatus());
        node.setCreateTime(category.getCreateTime());
        node.setUpdateTime(category.getUpdateTime());
        return node;
    }
}
