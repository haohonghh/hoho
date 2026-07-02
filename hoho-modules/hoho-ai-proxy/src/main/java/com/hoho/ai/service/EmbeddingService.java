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

    public EmbeddingResponse embed(EmbeddingRequest request)
    {
        String text = requireText(request == null ? null : request.getText(), "向量化文本不能为空");
        List<Double> vector = toVector(embeddingModel.embed(text));

        EmbeddingResponse response = new EmbeddingResponse();
        response.setDimension(resolveDimension(vector));
        response.setVector(vector);
        return response;
    }

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

    private List<String> normalizeTexts(List<String> texts)
    {
        List<String> result = new ArrayList<>();
        for (String text : texts)
        {
            result.add(requireText(text, "批量向量化文本中存在空值"));
        }
        return result;
    }

    private List<Double> toVector(float[] vector)
    {
        List<Double> result = new ArrayList<>(vector.length);
        for (float item : vector)
        {
            result.add((double) item);
        }
        return result;
    }

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
