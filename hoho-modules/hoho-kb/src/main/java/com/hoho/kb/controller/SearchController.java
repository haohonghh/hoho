package com.hoho.kb.controller;

import com.hoho.common.core.domain.R;
import com.hoho.kb.model.request.SearchRequest;
import com.hoho.kb.model.response.SearchResponse;
import com.hoho.kb.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识检索接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/kb/search")
public class SearchController
{
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService)
    {
        this.searchService = searchService;
    }

    @PostMapping("/vector")
    public R<SearchResponse> vectorSearch(@RequestBody SearchRequest request)
    {
        try
        {
            return R.ok(searchService.vectorSearch(request));
        }
        catch (Exception e)
        {
            log.error("向量检索失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/keyword")
    public R<SearchResponse> keywordSearch(@RequestBody SearchRequest request)
    {
        try
        {
            return R.ok(searchService.keywordSearch(request));
        }
        catch (Exception e)
        {
            log.error("关键词检索失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/hybrid")
    public R<SearchResponse> hybridSearch(@RequestBody SearchRequest request)
    {
        try
        {
            return R.ok(searchService.hybridSearch(request));
        }
        catch (Exception e)
        {
            log.error("混合检索失败", e);
            return R.fail(e.getMessage());
        }
    }
}
