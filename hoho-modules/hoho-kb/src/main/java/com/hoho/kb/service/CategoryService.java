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

    /**
     * 创建一个新的知识分类。
     * <p>
     * 支持创建根分类（parentId为空时设为0）和子分类。
     * 排序值越小越靠前，未指定时默认为0。
     *
     * @param request 分类请求参数，包含名称、父分类ID和排序值
     * @return 新增分类的主键ID
     * @throws IllegalArgumentException 当分类名称为空白时抛出
     */
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

    /**
     * 以树形结构查询所有分类。
     * <p>
     * 算法思路（经典的 "两次遍历建树法"）：
     * 1. 从数据库获取所有全量分类扁平列表
     * 2. 首次遍历：将所有分类转换为 CategoryTreeNode 并放入以 ID 为 key 的 Map
     * 3. 二次遍历：根据 parentId 将节点挂载到父节点的 children 中，
     *    找不到父节点的视为根节点
     * <p>
     * 注意：parentId 为 0 的分类会被视为根节点（与 create 方法中默认值保持一致）。
     *
     * @return 分类树的根节点列表，每个节点包含嵌套的子分类
     */
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

    /**
     * 将 Category 数据库实体转换为分类树节点 CategoryTreeNode。
     * <p>
     * 拷贝所有基础字段，用于树形结构展示。
     *
     * @param category 数据库分类实体
     * @return 树形结构节点
     */
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
