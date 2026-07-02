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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 检索服务
 *
 * @author hoho
 */
@Service
public class SearchService
{
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

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

    /**
     * 纯向量语义检索：将查询文本向量化后，在向量索引中进行近邻搜索。
     * <p>
     * 流程：
     * 1. 校验查询内容非空
     * 2. 解析 topK 参数（优先使用请求值，否则取配置的默认值）
     * 3. 调用AI嵌入服务获取查询文本的向量表示
     * 4. 使用向量在 EmbeddingMapper 中进行近似最近邻检索
     *
     * @param request 检索请求，包含 query 文本和可选的 topK
     * @return 检索响应，包含命中的问答知识列表（按相似度降序）
     */
    public SearchResponse vectorSearch(SearchRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getQuery()))
        {
            throw new IllegalArgumentException("检索内容不能为空");
        }
        int topK = request.getTopK() == null ? kbProperties.getSearch().getDefaultTopK() : request.getTopK();
        long start = System.currentTimeMillis();
        log.info("知识库向量检索开始 queryLength={}, topK={}", request.getQuery().length(), topK);
        List<Double> vector = aiProxyClient.embedding(request.getQuery());
        List<SearchItem> items = embeddingMapper.search(vector, Math.max(1, topK));

        SearchResponse response = new SearchResponse();
        response.setQuery(request.getQuery());
        response.setItems(items);
        log.info("知识库向量检索完成 queryLength={}, topK={}, itemCount={}, cost={}ms", request.getQuery().length(),
                topK, items == null ? 0 : items.size(), System.currentTimeMillis() - start);
        return response;
    }

    /**
     * 纯关键词检索：基于分词匹配在关键词索引中进行全文搜索。
     * <p>
     * 适用于精确词汇匹配的场景，检索速度较快但无法处理语义相似但表述不同的查询。
     *
     * @param request 检索请求，包含 query 文本和可选的 topK
     * @return 检索响应，按关键词匹配度降序排列
     */
    public SearchResponse keywordSearch(SearchRequest request)
    {
        validate(request);
        int topK = resolveTopK(request);
        long start = System.currentTimeMillis();
        log.info("知识库关键词检索开始 queryLength={}, topK={}", request.getQuery().length(), topK);
        List<SearchItem> items = keywordMapper.search(request.getQuery().trim(), topK);
        log.info("知识库关键词检索完成 queryLength={}, topK={}, itemCount={}, cost={}ms", request.getQuery().length(),
                topK, items == null ? 0 : items.size(), System.currentTimeMillis() - start);
        return response(request.getQuery(), items);
    }

    /**
     * 混合检索：同时执行关键词检索和向量语义检索，使用 RRF (Reciprocal Rank Fusion) 算法融合结果。
     * <p>
     * 核心设计：
     * 1. 扩大召回量（recallTopK = topK * 3），为融合阶段提供充足的候选集
     * 2. 分别执行关键词检索和向量检索，各取 recallTopK 条
     * 3. 通过 RRF 算法合并两路结果：每个文档的得分 = Σ 1/(k + rank)，k为平滑常数
     * 4. 将 RRF 得分与原始得分加权归一化，排序后取 topK
     * <p>
     * 优势：兼顾精确匹配和语义理解，在专业术语和同义改写场景均有较好表现。
     *
     * @param request 检索请求，包含 query 文本和可选的 topK
     * @return 融合后的检索响应，source 标记为 "hybrid"
     */
    public SearchResponse hybridSearch(SearchRequest request)
    {
        validate(request);
        int topK = resolveTopK(request);
        int recallTopK = Math.max(topK * 3, topK);
        long start = System.currentTimeMillis();
        log.info("知识库混合检索开始 queryLength={}, topK={}, recallTopK={}", request.getQuery().length(), topK,
                recallTopK);

        List<SearchItem> keywordItems = keywordMapper.search(request.getQuery().trim(), recallTopK);
        List<Double> vector = aiProxyClient.embedding(request.getQuery());
        List<SearchItem> vectorItems = embeddingMapper.search(vector, recallTopK);

        List<SearchItem> items = fuseByRrf(keywordItems, vectorItems, topK);
        log.info("知识库混合检索完成 queryLength={}, topK={}, keywordCount={}, vectorCount={}, itemCount={}, cost={}ms",
                request.getQuery().length(), topK, keywordItems == null ? 0 : keywordItems.size(),
                vectorItems == null ? 0 : vectorItems.size(), items == null ? 0 : items.size(),
                System.currentTimeMillis() - start);
        return response(request.getQuery(), items);
    }

    /**
     * 使用 RRF (Reciprocal Rank Fusion) 算法融合关键词检索和向量检索的结果。
     * <p>
     * RRF 公式：RRF_score(d) = Σ 1/(k + rank_i)，其中 k 为平滑常数（默认60），
     * rank_i 为文档 d 在第 i 路检索结果中的排名。
     * <p>
     * 最终得分由 RRF 归一化得分与原始归一化得分加权混合（各占50%），
     * 既保留排序位置信息，又兼顾原始相似度分数。
     *
     * @param keywordItems 关键词检索结果列表
     * @param vectorItems  向量检索结果列表
     * @param topK         最终返回的条目数
     * @return 融合、排序并截断后的检索结果列表
     */
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

    /**
     * 为单路检索结果累加 RRF 分数。
     * <p>
     * 遍历检索结果列表，按排名位置计算 1/(k + rank) 增量得分。
     * 同时记录每个文档的原始得分（取各路中的最大值）。
     *
     * @param items            单路检索结果
     * @param itemMap          文档ID到SearchItem的映射（去重，保留首次出现的条目）
     * @param scoreMap         文档ID到累计RRF得分的映射
     * @param originalScoreMap 文档ID到最大原始得分的映射
     * @param k                RRF 平滑常数
     */
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

    /**
     * 计算混合检索的最终归一化得分。
     * <p>
     * RRF 得分除以其理论最大值（2/(k+1)）归一化到 [0,1]，
     * 原始得分直接截断到 [0,1]，两者各取 50% 权重求和。
     *
     * @param rrfScore       该文档的累计 RRF 得分
     * @param originalScore  该文档的最大原始得分
     * @param maxRrf         单路 RRF 的理论最大值，用于归一化
     * @return 混合后的最终得分，范围 [0, 1]
     */
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
