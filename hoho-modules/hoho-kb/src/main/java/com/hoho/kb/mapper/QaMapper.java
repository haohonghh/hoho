package com.hoho.kb.mapper;

import java.util.List;
import java.util.Optional;

import com.hoho.kb.domain.Qa;
import org.apache.ibatis.annotations.Param;

/**
 * 问答知识数据层
 *
 * @author hoho
 */
public interface QaMapper
{
    int insert(Qa qa);

    int update(Qa qa);

    Qa selectById(Long id);

    List<Qa> list();

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    default Optional<Qa> findById(Long id)
    {
        return Optional.ofNullable(selectById(id));
    }
}
