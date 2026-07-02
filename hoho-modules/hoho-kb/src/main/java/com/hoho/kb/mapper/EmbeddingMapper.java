package com.hoho.kb.mapper;

import java.util.List;

import com.hoho.kb.model.response.SearchItem;
import org.apache.ibatis.annotations.Param;

/**
 * 知识向量数据层
 *
 * @author hoho
 */
public interface EmbeddingMapper
{
    int deleteByQaId(Long qaId);

    int insertVector(@Param("qaId") Long qaId, @Param("contentType") String contentType,
            @Param("content") String content, @Param("vector") String vector, @Param("model") String model);

    List<SearchItem> searchByVector(@Param("vector") String vector, @Param("topK") int topK);

    default void replaceQaEmbedding(Long qaId, String content, List<Double> vector, String model)
    {
        deleteByQaId(qaId);
        insertVector(qaId, "qa", content, toVectorLiteral(vector), model);
    }

    default List<SearchItem> search(List<Double> vector, int topK)
    {
        return searchByVector(toVectorLiteral(vector), topK);
    }

    private String toVectorLiteral(List<Double> vector)
    {
        if (vector == null || vector.isEmpty())
        {
            throw new IllegalArgumentException("向量不能为空");
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++)
        {
            if (i > 0)
            {
                builder.append(',');
            }
            builder.append(vector.get(i));
        }
        return builder.append(']').toString();
    }
}
