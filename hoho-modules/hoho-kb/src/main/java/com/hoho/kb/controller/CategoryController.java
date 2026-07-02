package com.hoho.kb.controller;

import java.util.List;

import com.hoho.common.core.domain.R;
import com.hoho.kb.model.request.CategoryRequest;
import com.hoho.kb.model.response.CategoryTreeNode;
import com.hoho.kb.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识分类接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/kb/category")
public class CategoryController
{
    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    @PostMapping
    public R<Long> create(@RequestBody CategoryRequest request)
    {
        try
        {
            return R.ok(categoryService.create(request));
        }
        catch (Exception e)
        {
            log.error("创建知识分类失败", e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/tree")
    public R<List<CategoryTreeNode>> tree()
    {
        return R.ok(categoryService.tree());
    }
}
