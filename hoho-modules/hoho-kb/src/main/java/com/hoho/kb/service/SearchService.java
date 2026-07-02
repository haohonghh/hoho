package com.hoho.kb.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoho.common.core.utils.StringUtils;
import com.hoho.kb.config.KbProperties;
import com.hoho.kb.mapper.EmbeddingMapper;
import com.hoho.kb.mapper.KeywordMapper;
import com.hoho.kb.model.request.SearchRequest;
import com.hoho.kb.model.response.SearchItem;
import com.hoho.kb.model.response.SearchResponse;
import org.springframework.stereotype.Service;

/**
 * 检索服务
 *
 * @author hoho
 */
@Service
public class SearchService
{
    private final AiProxyClient aiProxyClient;

    private final EmbeddingMapper embeddingMapper;

    private final KeywordMapper keywordMapper;

    private final KbProperties kbProperties;

    public SearchService(AiProxyClient aiProxyClient, EmbeddingMapper embeddingMapper,
            KeywordMapper keywordMapper, KbProperties kbProperties)
    {
        this.aiProxyClient = aiProxyClient;
        this.embeddingMapper = embeddingMapper;
        this.keywordMapper = keywordMapper;
        this.kbProperties = kbProperties;
    }

    public SearchResponse vectorSearch(SearchRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getQuery()))
        {
            throw new IllegalArgumentException("检索内容不能为空");
        }
        int topK = request.getTopK() == null ? kbProperties.getSearch().getDefaultTopK() : request.getTopK();
        List<Double> vector = aiProxyClient.embedding(request.getQuery());
        List<SearchItem> items = embeddingMapper.search(vector, Math.max(1, topK));

        SearchResponse response = new SearchResponse();
        response.setQuery(request.getQuery());
        response.setItems(items);
        return response;
    }

    public SearchResponse keywordSearch(SearchRequest request)
    {
        validate(request);
        int topK = resolveTopK(request);
        List<SearchItem> items = keywordMapper.search(request.getQuery().trim(), topK);
        return response(request.getQuery(), items);
    }

    public SearchResponse hybridSearch(SearchRequest request)
    {
        validate(request);
        int topK = resolveTopK(request);
        int recallTopK = Math.max(topK * 3, topK);

        List<SearchItem> keywordItems = keywordMapper.search(request.getQuery().trim(), recallTopK);
        List<Double> vector = aiProxyClient.embedding(request.getQuery());
        List<SearchItem> vectorItems = embeddingMapper.search(vector, recallTopK);

        List<SearchItem> items = fuseByRrf(keywordItems, vectorItems, topK);
        return response(request.getQuery(), items);
    }

    private List<SearchItem> fuseByRrf(List<SearchItem> keywordItems, List<SearchItem> vectorItems, int topK)
    {
        Map<Long, SearchItem> itemMap = new LinkedHashMap<>();
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        Map<Long, Double> originalScoreMap = new LinkedHashMap<>();
        int rrfK = 60;
        addRrfScore(keywordItems, itemMap, scoreMap, originalScoreMap, rrfK);
        addRrfScore(vectorItems, itemMap, scoreMap, originalScoreMap, rrfK);
        double maxRrf = 2D / (rrfK + 1);

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    SearchItem item = itemMap.get(entry.getKey());
                    SearchItem fused = copy(item);
                    fused.setScore(normalizeHybridScore(entry.getValue(), originalScoreMap.get(entry.getKey()), maxRrf));
                    fused.setSource("hybrid");
                    return fused;
                })
                .toList();
    }

    private void addRrfScore(List<SearchItem> items, Map<Long, SearchItem> itemMap, Map<Long, Double> scoreMap,
            Map<Long, Double> originalScoreMap, int k)
    {
        if (items == null)
        {
            return;
        }
        for (int i = 0; i < items.size(); i++)
        {
            SearchItem item = items.get(i);
            if (item.getQaId() == null)
            {
                continue;
            }
            itemMap.putIfAbsent(item.getQaId(), item);
            scoreMap.merge(item.getQaId(), 1D / (k + i + 1), Double::sum);
            originalScoreMap.merge(item.getQaId(), item.getScore() == null ? 0D : item.getScore(), Math::max);
        }
    }

    private double normalizeHybridScore(double rrfScore, Double originalScore, double maxRrf)
    {
        double rrfNormalized = maxRrf <= 0 ? 0D : Math.min(1D, rrfScore / maxRrf);
        double originalNormalized = Math.max(0D, Math.min(1D, originalScore == null ? 0D : originalScore));
        return Math.min(1D, rrfNormalized * 0.5D + originalNormalized * 0.5D);
    }

    private SearchItem copy(SearchItem item)
    {
        SearchItem copy = new SearchItem();
        copy.setQaId(item.getQaId());
        copy.setQuestion(item.getQuestion());
        copy.setAnswer(item.getAnswer());
        copy.setScore(item.getScore());
        copy.setSource(item.getSource());
        return copy;
    }

    private void validate(SearchRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getQuery()))
        {
            throw new IllegalArgumentException("检索内容不能为空");
        }
    }

    private int resolveTopK(SearchRequest request)
    {
        return Math.max(1, request.getTopK() == null ? kbProperties.getSearch().getDefaultTopK() : request.getTopK());
    }

    private SearchResponse response(String query, List<SearchItem> items)
    {
        SearchResponse response = new SearchResponse();
        response.setQuery(query);
        response.setItems(new ArrayList<>(items));
        return response;
    }
}
