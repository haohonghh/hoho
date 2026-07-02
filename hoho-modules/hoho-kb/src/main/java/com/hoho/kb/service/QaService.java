package com.hoho.kb.service;

import java.util.List;

import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.config.KbProperties;
import com.hoho.kb.domain.Qa;
import com.hoho.kb.mapper.EmbeddingMapper;
import com.hoho.kb.mapper.QaMapper;
import com.hoho.kb.model.request.QaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(QaService.class);

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

    /**
     * 创建一条问答知识记录。
     *
     * @param request 问答请求参数，包含分类ID、问题、答案及相似问法
     * @return 新增记录的主键ID
     */
    public Long create(QaRequest request)
    {
        validate(request);
        Qa qa = buildQa(null, request);
        qaMapper.insert(qa);
        return qa.getId();
    }

    /**
     * 更新指定ID的问答知识记录。
     * <p>
     * 注意：更新操作仅修改数据库基础字段，不会刷新向量索引。若问题或答案发生变更，
     * 需要重新调用 {@link #publish(Long)} 以更新向量化索引。
     *
     * @param id      待更新的问答知识主键ID
     * @param request 更新后的问答内容
     * @throws IllegalArgumentException 当目标记录不存在时抛出
     */
    public void update(Long id, QaRequest request)
    {
        validateId(id);
        validate(request);
        if (qaMapper.update(buildQa(id, request)) == 0)
        {
            throw new IllegalArgumentException("问答知识不存在");
        }
    }

    /**
     * 根据ID查询单条问答知识记录。
     *
     * @param id 问答知识主键ID
     * @return 对应的问答知识实体
     * @throws IllegalArgumentException 当ID非法或记录不存在时抛出
     */
    public Qa get(Long id)
    {
        validateId(id);
        return qaMapper.findById(id).orElseThrow(() -> new IllegalArgumentException("问答知识不存在"));
    }

    /**
     * 查询所有问答知识记录列表。
     *
     * @return 全量问答知识列表
     */
    public List<Qa> list()
    {
        return qaMapper.list();
    }

    /**
     * 发布一条问答知识，将其向量化并存入向量索引中。
     * <p>
     * 核心流程：
     * 1. 加载问答实体
     * 2. 拼接"问题 + 答案 + 相似问法"文本
     * 3. 调用AI嵌入服务获取向量表示
     * 4. 校验向量维度与配置一致
     * 5. 使用 replace 语义写入向量索引（若已存在则覆盖）
     * 6. 将问答状态更新为 published
     * <p>
     * 整个操作在事务中执行，任一步骤失败都会回滚。
     *
     * @param id 待发布的问答知识主键ID
     * @throws IllegalStateException 当向量维度与配置不匹配或AI服务异常时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id)
    {
        long start = System.currentTimeMillis();
        log.info("发布问答知识开始 qaId={}", id);
        Qa qa = get(id);
        List<Double> vector = aiProxyClient.embedding(buildEmbeddingContent(qa));
        if (vector.size() != kbProperties.getEmbedding().getDimension())
        {
            throw new IllegalStateException("向量维度不匹配，期望 " + kbProperties.getEmbedding().getDimension() + "，实际 " + vector.size());
        }
        embeddingMapper.replaceQaEmbedding(qa.getId(), buildEmbeddingContent(qa), vector, kbProperties.getEmbedding().getModel());
        qaMapper.updateStatus(id, "published");
        log.info("发布问答知识完成 qaId={}, dimension={}, model={}, cost={}ms", id, vector.size(),
                kbProperties.getEmbedding().getModel(), System.currentTimeMillis() - start);
    }

    /**
     * 将指定的问答知识下线，状态变更为 offline。
     * <p>
     * 注意：此方法仅修改状态字段，不会主动删除向量索引中的记录。
     * 检索服务应在查询时过滤掉非 published 状态的记录。
     *
     * @param id 待下线的问答知识主键ID
     * @throws IllegalArgumentException 当目标记录不存在时抛出
     */
    public void offline(Long id)
    {
        validateId(id);
        if (qaMapper.updateStatus(id, "offline") == 0)
        {
            throw new IllegalArgumentException("问答知识不存在");
        }
    }

    /**
     * 将请求参数转换为 Qa 实体对象。
     *
     * @param id      主键ID，新建时传 null
     * @param request 请求参数
     * @return 组装好的 Qa 实体
     */
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

    /**
     * 构建向量化内容文本，将问答知识拼接为单一字符串供嵌入模型使用。
     * <p>
     * 拼接格式：问题 + 换行 + 答案 + 换行 + 相似问法（可选）。
     * 相似问法仅在有内容时追加，避免多余的换行干扰向量表示。
     *
     * @param qa 问答知识实体
     * @return 拼接后的向量化文本
     */
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
