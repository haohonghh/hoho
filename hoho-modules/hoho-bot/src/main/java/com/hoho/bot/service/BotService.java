package com.hoho.bot.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.hoho.bot.config.BotProperties;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.AiChatResponse;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.model.response.KbSearchResponse;
import com.hoho.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 文本机器人服务
 *
 * @author hoho
 */
@Service
public class BotService
{
    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    private final KbClient kbClient;

    private final AiProxyClient aiProxyClient;

    private final AiProxyStreamClient aiProxyStreamClient;

    private final BotProperties botProperties;

    private final ConversationRecordService conversationRecordService;

    private final LongTermMemoryCaptureService longTermMemoryCaptureService;

    public BotService(KbClient kbClient, AiProxyClient aiProxyClient, AiProxyStreamClient aiProxyStreamClient, BotProperties botProperties,
            ConversationRecordService conversationRecordService, LongTermMemoryCaptureService longTermMemoryCaptureService)
    {
        this.kbClient = kbClient;
        this.aiProxyClient = aiProxyClient;
        this.aiProxyStreamClient = aiProxyStreamClient;
        this.botProperties = botProperties;
        this.conversationRecordService = conversationRecordService;
        this.longTermMemoryCaptureService = longTermMemoryCaptureService;
    }

    /**
     * 机器人对话入口方法，实现三级路由策略来回答用户问题。
     *
     * <p>核心处理流程如下：
     * <ol>
     *   <li>校验请求参数；</li>
     *   <li>解析（或新建）会话编号，并持久化用户消息；</li>
     *   <li>使用用户消息对知识库进行混合检索，取 score 最高的一条作为 best 匹配；</li>
     *   <li>根据 best 的相似度分数走三条路径之一：
     *       <ul>
     *         <li>分数 ≥ directMinScore → 直接使用知识库答案（kb）；</li>
     *         <li>分数 ≥ assistMinScore  → 将知识库资料注入 system prompt，让 AI 辅助作答（ai_with_kb）；</li>
     *         <li>分数 &lt; assistMinScore → 仅使用兜底 system prompt，由 AI 自由作答（ai）。</li>
     *       </ul>
     *   </li>
     *   <li>持久化回复内容并返回给调用方。</li>
     * </ol>
     *
     * @param request 对话请求，包含会话编号（可为空，空则自动生成）、用户消息、topK 检索数量
     * @return BotChatResponse 包含生成的回答、答案来源（kb / ai_with_kb / ai）、相似度分数、参考条目列表
     */
    public BotChatResponse chat(BotChatRequest request)
    {
        validate(request);

        String conversationId = resolveConversationId(request.getConversationId());
        long start = System.currentTimeMillis();
        log.info("Bot对话开始 conversationId={}, messageLength={}, requestTopK={}", conversationId,
                request.getMessage().length(), request.getTopK());
        // 先将用户消息持久化（同时保证会话存在）
        conversationRecordService.recordUserMessage(conversationId, request.getMessage());

        // 取用户自定义的 topK，若未传则使用配置默认值；最小为 1
        int topK = request.getTopK() == null ? botProperties.getAnswer().getTopK() : request.getTopK();
        List<KbSearchItem> references = searchReferences(request.getMessage(), Math.max(1, topK), conversationId);

        // 从知识库检索结果中取出相似度分数最高的一条，作为后续路由的判断依据
        KbSearchItem best = references.stream()
                .max(Comparator.comparing(item -> item.getScore() == null ? 0D : item.getScore()))
                .orElse(null);

        // 路径一：分数达到直接回答阈值，直接返回知识库答案，无需调用 AI
        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getDirectMinScore())
        {
            return recordAndReturn(fromKb(conversationId, best, references), request.getMessage(), true, start);
        }

        // 路径二：分数达到辅助阈值，将知识库资料注入 system prompt，调用 AI 生成回答
        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getAssistMinScore())
        {
            return aiWithFallback(conversationId, buildAssistSystemPrompt(best), request.getMessage(),
                    fromAiWithKbSource(conversationId, best, references), start);
        }

        // 路径三：知识库未命中，仅用兜底 system prompt 让 AI 生成回答
        return aiWithFallback(conversationId, botProperties.getAnswer().getFallbackSystemPrompt(), request.getMessage(),
                fromAiSource(conversationId, references), start);
    }

    /**
     * 流式对话入口。
     *
     * <p>业务层流式接口。会先完成会话编号解析、用户消息持久化与知识库路由判断；
     * 若命中高分知识库则直接输出单片答案，若进入 AI 路径则透传 AI 代理的逐片输出，
     * 并在流结束时将完整助手回复落库。</p>
     *
     * @param request 对话请求
     * @return 文本分片事件流
     */
    public Flux<String> streamChat(BotChatRequest request)
    {
        validate(request);

        String conversationId = resolveConversationId(request.getConversationId());
        long start = System.currentTimeMillis();
        log.info("Bot流式对话开始 conversationId={}, messageLength={}, requestTopK={}", conversationId,
                request.getMessage().length(), request.getTopK());
        conversationRecordService.recordUserMessage(conversationId, request.getMessage());

        int topK = request.getTopK() == null ? botProperties.getAnswer().getTopK() : request.getTopK();
        List<KbSearchItem> references = searchReferences(request.getMessage(), Math.max(1, topK), conversationId);
        KbSearchItem best = references.stream()
                .max(Comparator.comparing(item -> item.getScore() == null ? 0D : item.getScore()))
                .orElse(null);

        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getDirectMinScore())
        {
            BotChatResponse response = recordAndReturn(fromKb(conversationId, best, references), request.getMessage(), true, start);
            return Flux.just(response.getAnswer());
        }

        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getAssistMinScore())
        {
            return streamAiWithFallback(conversationId, buildAssistSystemPrompt(best), request.getMessage(),
                    fromAiWithKbSource(conversationId, best, references), start);
        }

        return streamAiWithFallback(conversationId, botProperties.getAnswer().getFallbackSystemPrompt(),
                request.getMessage(), fromAiSource(conversationId, references), start);
    }

    /**
     * 构建"知识库直接命中"的响应对象。
     *
     * <p>当知识库最佳匹配的分数达到 {@code directMinScore} 时调用此方法，
     * 直接使用知识库中预置的答案作答，答案来源标记为 {@code kb}。
     *
     * @param conversationId 当前会话编号
     * @param best           知识库中相似度分数最高的检索条目
     * @param references     本次检索返回的所有参考条目（含 best）
     * @return BotChatResponse 以知识库答案填充的响应对象
     */
    private BotChatResponse fromKb(String conversationId, KbSearchItem best, List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setAnswer(best.getAnswer());
        response.setSource("kb");
        response.setScore(best.getScore());
        response.setReferences(references);
        return response;
    }

    /**
     * 构建"AI 借助知识库资料"生成的响应对象。
     *
     * <p>当知识库最佳匹配的分数介于 {@code assistMinScore} 与 {@code directMinScore} 之间时调用此方法，
     * AI 会参考知识库资料来回答用户问题，答案来源标记为 {@code ai_with_kb}。
     *
     * @param conversationId 当前会话编号
     * @param aiProxyClient 返回的 AI 对话结果（content 字段为 AI 生成的正文）
     * @param best           知识库中相似度分数最高的检索条目，其分数将作为本次回答的可信度参考
     * @param references     本次检索返回的所有参考条目
     * @return BotChatResponse 以 AI 生成内容填充的响应对象
     */
    private BotChatResponse fromAiWithKb(String conversationId, AiChatResponse aiResponse, KbSearchItem best,
            List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setAnswer(aiResponse == null ? null : aiResponse.getContent());
        response.setSource("ai_with_kb");
        response.setScore(best.getScore());
        response.setReferences(references);
        return response;
    }

    private BotChatResponse fromAiWithKbSource(String conversationId, KbSearchItem best, List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setSource("ai_with_kb");
        response.setScore(best == null ? null : best.getScore());
        response.setReferences(references);
        return response;
    }

    /**
     * 构建"纯 AI 兜底"的响应对象。
     *
     * <p>当知识库无任何有效命中（分数低于 {@code assistMinScore}）时调用此方法，
     * 仅使用兜底 system prompt 让 AI 生成回答，答案来源标记为 {@code ai}，不携带分数。
     *
     * @param conversationId 当前会话编号
     * @param aiProxyClient 返回的 AI 对话结果
     * @param references     本次检索返回的参考条目（通常为空列表或低分条目）
     * @return BotChatResponse 以 AI 生成内容填充的响应对象
     */
    private BotChatResponse fromAi(String conversationId, AiChatResponse aiResponse, List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setAnswer(aiResponse == null ? null : aiResponse.getContent());
        response.setSource("ai");
        response.setScore(null);
        response.setReferences(references);
        return response;
    }

    private BotChatResponse fromAiSource(String conversationId, List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setSource("ai");
        response.setScore(null);
        response.setReferences(references);
        return response;
    }

    private BotChatResponse fromFallback(String conversationId, List<KbSearchItem> references)
    {
        BotChatResponse response = new BotChatResponse();
        response.setConversationId(conversationId);
        response.setAnswer(botProperties.getAnswer().getServiceDegradeReply());
        response.setSource("fallback");
        response.setScore(null);
        response.setReferences(references);
        return response;
    }

    /**
     * 持久化助手回复并原样返回响应对象。
     *
     * <p>在生成回答后统一调用此方法，确保所有路径（kb / ai_with_kb / ai）都写入消息记录与摘要更新。
     *
     * @param response 已经填充好的响应对象
     * @return 同一个 response 对象（用于方法链式返回）
     */
    private BotChatResponse recordAndReturn(BotChatResponse response, String userMessage, boolean appendMemory, long start)
    {
        conversationRecordService.recordAssistantMessage(response.getConversationId(), response);
        if (appendMemory)
        {
            try
            {
                aiProxyClient.appendMemory(response.getConversationId(), userMessage, response.getAnswer());
            }
            catch (Exception e)
            {
                log.warn("追加短期记忆失败，但本次回答继续返回 conversationId={}, reason={}",
                        response.getConversationId(), e.getMessage());
            }
        }
        log.info("Bot对话完成 conversationId={}, source={}, score={}, referenceCount={}, appendMemory={}, cost={}ms",
                response.getConversationId(), response.getSource(), response.getScore(),
                response.getReferences() == null ? 0 : response.getReferences().size(), appendMemory,
                System.currentTimeMillis() - start);
        longTermMemoryCaptureService.captureIfNecessary(response.getConversationId(), userMessage, response);
        return response;
    }

    private List<KbSearchItem> searchReferences(String message, int topK, String conversationId)
    {
        try
        {
            KbSearchResponse kbSearchResponse = kbClient.hybridSearch(message, topK);
            return kbSearchResponse == null || kbSearchResponse.getItems() == null
                    ? Collections.emptyList()
                    : kbSearchResponse.getItems();
        }
        catch (Exception e)
        {
            log.warn("知识库检索失败，降级走AI兜底 conversationId={}, reason={}", conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private BotChatResponse aiWithFallback(String conversationId, String systemPrompt, String message,
            BotChatResponse template, long start)
    {
        try
        {
            AiChatResponse aiResponse = aiProxyClient.chat(conversationId, systemPrompt, message);
            template.setAnswer(aiResponse == null ? null : aiResponse.getContent());
            return recordAndReturn(template, message, false, start);
        }
        catch (Exception e)
        {
            log.warn("AI调用失败，返回固定降级话术 conversationId={}, reason={}", conversationId, e.getMessage());
            return recordAndReturn(fromFallback(conversationId, template.getReferences()), message, false, start);
        }
    }

    private Flux<String> streamAiWithFallback(String conversationId, String systemPrompt, String message,
            BotChatResponse template, long start)
    {
        StringBuilder answerBuilder = new StringBuilder();
        return aiProxyStreamClient.streamChat(conversationId, systemPrompt, message)
                .doOnNext(answerBuilder::append)
                .doOnComplete(() -> {
                    template.setAnswer(answerBuilder.toString());
                    recordAndReturn(template, message, false, start);
                })
                .onErrorResume(error -> {
                    log.warn("AI流式调用失败，返回固定降级话术 conversationId={}, reason={}", conversationId,
                            error.getMessage());
                    BotChatResponse fallback = recordAndReturn(fromFallback(conversationId, template.getReferences()),
                            message, false, start);
                    return Flux.just(fallback.getAnswer());
                });
    }

    /**
     * 根据知识库最佳匹配条目构造辅助模式的 system prompt。
     *
     * <p>将知识库中的"问题"和"答案"注入到 system prompt 中，告知 AI
     * 优先使用这些资料回答用户问题，并要求回答准确、简洁、可执行；
     * 资料不足时需明确说明，避免编造信息。
     *
     * @param best 知识库中相似度分数最高的检索条目，用于提取 question 和 answer
     * @return 拼接好的 system prompt 字符串
     */
    private String buildAssistSystemPrompt(KbSearchItem best)
    {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是企业IT运维智能客服助手。请优先依据以下知识库资料回答用户问题，回答要准确、简洁、可执行；");
        prompt.append("如果资料不足以确定结论，请说明需要补充的信息，不要编造。\n");
        prompt.append("知识库问题：").append(best.getQuestion()).append('\n');
        prompt.append("知识库答案：").append(best.getAnswer());
        return prompt.toString();
    }

    /**
     * 解析会话编号：若调用方未传则生成一个随机 UUID。
     *
     * <p>每次会话首条消息到来时，由该机制保证服务端始终拥有一个可用的会话标识，
     * 后续同一会话内的多条消息使用同一 conversationId 进行持久化。
     *
     * @param conversationId 请求中携带的会话编号，可为空
     * @return 有效的会话编号（原值或新生成的 UUID 字符串）
     */
    private String resolveConversationId(String conversationId)
    {
        return StringUtils.isBlank(conversationId) ? UUID.randomUUID().toString() : conversationId;
    }

    /**
     * 校验对话请求的合法性。
     *
     * @param request 对话请求；为 null 或 message 为空/空白时抛出 IllegalArgumentException
     */
    private void validate(BotChatRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getMessage()))
        {
            throw new IllegalArgumentException("消息内容不能为空");
        }
    }
}
