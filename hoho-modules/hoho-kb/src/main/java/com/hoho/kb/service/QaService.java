package com.hoho.kb.service;

import java.util.List;

import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.config.KbProperties;
import com.hoho.kb.domain.Qa;
import com.hoho.kb.mapper.EmbeddingMapper;
import com.hoho.kb.mapper.QaMapper;
import com.hoho.kb.model.request.QaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答知识服务
 *
 * @author hoho
 */
@Service
public class QaService
{
    private final QaMapper qaMapper;

    private final EmbeddingMapper embeddingMapper;

    private final AiProxyClient aiProxyClient;

    private final KbProperties kbProperties;

    public QaService(QaMapper qaMapper, EmbeddingMapper embeddingMapper, AiProxyClient aiProxyClient,
            KbProperties kbProperties)
    {
        this.qaMapper = qaMapper;
        this.embeddingMapper = embeddingMapper;
        this.aiProxyClient = aiProxyClient;
        this.kbProperties = kbProperties;
    }

    public Long create(QaRequest request)
    {
        validate(request);
        Qa qa = buildQa(null, request);
        qaMapper.insert(qa);
        return qa.getId();
    }

    public void update(Long id, QaRequest request)
    {
        validateId(id);
        validate(request);
        if (qaMapper.update(buildQa(id, request)) == 0)
        {
            throw new IllegalArgumentException("问答知识不存在");
        }
    }

    public Qa get(Long id)
    {
        validateId(id);
        return qaMapper.findById(id).orElseThrow(() -> new IllegalArgumentException("问答知识不存在"));
    }

    public List<Qa> list()
    {
        return qaMapper.list();
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id)
    {
        Qa qa = get(id);
        List<Double> vector = aiProxyClient.embedding(buildEmbeddingContent(qa));
        if (vector.size() != kbProperties.getEmbedding().getDimension())
        {
            throw new IllegalStateException("向量维度不匹配，期望 " + kbProperties.getEmbedding().getDimension() + "，实际 " + vector.size());
        }
        embeddingMapper.replaceQaEmbedding(qa.getId(), buildEmbeddingContent(qa), vector, kbProperties.getEmbedding().getModel());
        qaMapper.updateStatus(id, "published");
    }

    public void offline(Long id)
    {
        validateId(id);
        if (qaMapper.updateStatus(id, "offline") == 0)
        {
            throw new IllegalArgumentException("问答知识不存在");
        }
    }

    private Qa buildQa(Long id, QaRequest request)
    {
        Qa qa = new Qa();
        qa.setId(id);
        qa.setCategoryId(request.getCategoryId());
        qa.setQuestion(request.getQuestion());
        qa.setAnswer(request.getAnswer());
        qa.setSimilarQuestions(request.getSimilarQuestions());
        return qa;
    }

    private String buildEmbeddingContent(Qa qa)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(qa.getQuestion()).append('\n').append(qa.getAnswer());
        if (StringUtils.isNotBlank(qa.getSimilarQuestions()))
        {
            builder.append('\n').append(qa.getSimilarQuestions());
        }
        return builder.toString();
    }

    private void validate(QaRequest request)
    {
        if (request == null)
        {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getCategoryId() == null)
        {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        if (StringUtils.isBlank(request.getQuestion()))
        {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (StringUtils.isBlank(request.getAnswer()))
        {
            throw new IllegalArgumentException("答案不能为空");
        }
    }

    private void validateId(Long id)
    {
        if (id == null || id <= 0)
        {
            throw new IllegalArgumentException("ID非法");
        }
    }
}
