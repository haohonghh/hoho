package com.hoho.kb.mapper;

import java.util.List;

import com.hoho.kb.domain.Category;

/**
 * 知识分类数据层
 *
 * @author hoho
 */
public interface CategoryMapper
{
    int insert(Category category);

    List<Category> list();
}
