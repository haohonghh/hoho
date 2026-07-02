package com.hoho.kb.mapper;

import java.util.List;

import com.hoho.kb.model.response.SearchItem;
import org.apache.ibatis.annotations.Param;

/**
 * 关键词检索数据层
 *
 * @author hoho
 */
public interface KeywordMapper
{
    List<SearchItem> searchByKeyword(@Param("keyword") String keyword, @Param("topK") int topK);

    default List<SearchItem> search(String query, int topK)
    {
        return searchByKeyword("%" + escapeLike(query) + "%", topK);
    }

    private String escapeLike(String value)
    {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
