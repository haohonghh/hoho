package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.ai.config.AiProxyProperties;
import com.hoho.ai.model.request.BatchEmbeddingRequest;
import com.hoho.ai.model.request.EmbeddingRequest;
import com.hoho.ai.model.response.BatchEmbeddingResponse;
import com.hoho.ai.model.response.EmbeddingResponse;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * 向量化服务
 *
 * @author hoho
 */
@Service
public class EmbeddingService
{
    private final EmbeddingModel embeddingModel;

    private final AiProxyProperties aiProxyProperties;

    public EmbeddingService(EmbeddingModel embeddingModel, AiProxyProperties aiProxyProperties)
    {
        this.embeddingModel = embeddingModel;
        this.aiProxyProperties = aiProxyProperties;
    }

    /**
     * 对单条文本执行向量化（Embedding）。
     * <p>
     * 流程：
     * 1. 校验输入文本非空
     * 2. 调用底层 EmbeddingModel 获取 float[] 向量
     * 3. 将 float[] 转换为 List<Double> 以便 JSON 序列化
     * 4. 封装维度与向量数据返回
     * </p>
     *
     * @param request 向量化请求，包含待处理的文本
     * @return 包含向量维度与向量值列表的响应对象
     */
    public EmbeddingResponse embed(EmbeddingRequest request)
    {
        String text = requireText(request == null ? null : request.getText(), "向量化文本不能为空");
        List<Double> vector = toVector(embeddingModel.embed(text));

        EmbeddingResponse response = new EmbeddingResponse();
        response.setDimension(resolveDimension(vector));
        response.setVector(vector);
        return response;
    }

    /**
     * 批量对多条文本执行向量化，支持分批次调用底层模型。
     * <p>
     * 设计要点：
     * 1. 先对全部文本做非空校验（normalizeTexts），避免部分请求失败
     * 2. 按配置的 batchSize 分片，逐批调用 EmbeddingModel，
     *    防止单次请求文本过多导致模型超时或超限
     * 3. 每个结果项保留原始索引（index），便于调用方对齐输入/输出
     * 4. 维度信息优先取实际向量长度，若结果为空则回落到配置默认值
     * </p>
     *
     * @param request 批量向量化请求，包含待处理的文本列表
     * @return 包含各文本对应向量及统一维度的批量响应对象
     */
    public BatchEmbeddingResponse embedBatch(BatchEmbeddingRequest request)
    {
        if (request == null || request.getTexts() == null || request.getTexts().isEmpty())
        {
            throw new IllegalArgumentException("批量向量化文本不能为空");
        }

        List<String> texts = normalizeTexts(request.getTexts());
        List<BatchEmbeddingResponse.Item> items = new ArrayList<>();
        int batchSize = Math.max(1, aiProxyProperties.getEmbedding().getBatchSize());

        for (int from = 0; from < texts.size(); from += batchSize)
        {
            int to = Math.min(from + batchSize, texts.size());
            List<float[]> vectors = embeddingModel.embed(texts.subList(from, to));
            for (int i = 0; i < vectors.size(); i++)
            {
                BatchEmbeddingResponse.Item item = new BatchEmbeddingResponse.Item();
                item.setIndex(from + i);
                item.setVector(toVector(vectors.get(i)));
                items.add(item);
            }
        }

        BatchEmbeddingResponse response = new BatchEmbeddingResponse();
        response.setDimension(items.isEmpty() ? aiProxyProperties.getEmbedding().getDimension() : items.get(0).getVector().size());
        response.setItems(items);
        return response;
    }

    public String modelName()
    {
        return embeddingModel.getClass().getName();
    }

    /**
     * 对批量文本进行非空校验与 trim 处理。
     * 任何一条为空都会直接抛出异常，保证进入模型的文本都是有效的。
     *
     * @param texts 原始文本列表
     * @return 校验并 trim 后的文本列表
     */
    private List<String> normalizeTexts(List<String> texts)
    {
        List<String> result = new ArrayList<>();
        for (String text : texts)
        {
            result.add(requireText(text, "批量向量化文本中存在空值"));
        }
        return result;
    }

    /**
     * 将底层模型返回的 float[] 转换为 List<Double>。
     * 使用 Double 类型便于 JSON 序列化及下游数值处理。
     *
     * @param vector 模型输出的原始 float 数组
     * @return 包装后的 Double 列表
     */
    private List<Double> toVector(float[] vector)
    {
        List<Double> result = new ArrayList<>(vector.length);
        for (float item : vector)
        {
            result.add((double) item);
        }
        return result;
    }

    /**
     * 解析向量维度。
     * 当模型未返回有效向量时，回落到配置文件中预设的维度值。
     *
     * @param vector 向量化结果
     * @return 实际向量长度或配置的默认维度
     */
    private int resolveDimension(List<Double> vector)
    {
        return vector == null || vector.isEmpty() ? aiProxyProperties.getEmbedding().getDimension() : vector.size();
    }

    private String requireText(String value, String message)
    {
        if (StringUtils.isBlank(value))
        {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
