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
import org.springframework.stereotype.Service;

/**
 * 文本机器人服务
 *
 * @author hoho
 */
@Service
public class BotService
{
    private final KbClient kbClient;

    private final AiProxyClient aiProxyClient;

    private final BotProperties botProperties;

    private final ConversationRecordService conversationRecordService;

    public BotService(KbClient kbClient, AiProxyClient aiProxyClient, BotProperties botProperties,
            ConversationRecordService conversationRecordService)
    {
        this.kbClient = kbClient;
        this.aiProxyClient = aiProxyClient;
        this.botProperties = botProperties;
        this.conversationRecordService = conversationRecordService;
    }

    public BotChatResponse chat(BotChatRequest request)
    {
        validate(request);

        String conversationId = resolveConversationId(request.getConversationId());
        conversationRecordService.recordUserMessage(conversationId, request.getMessage());

        int topK = request.getTopK() == null ? botProperties.getAnswer().getTopK() : request.getTopK();
        KbSearchResponse kbSearchResponse = kbClient.hybridSearch(request.getMessage(), Math.max(1, topK));
        List<KbSearchItem> references = kbSearchResponse == null || kbSearchResponse.getItems() == null
                ? Collections.emptyList()
                : kbSearchResponse.getItems();

        KbSearchItem best = references.stream()
                .max(Comparator.comparing(item -> item.getScore() == null ? 0D : item.getScore()))
                .orElse(null);
        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getDirectMinScore())
        {
            return recordAndReturn(fromKb(conversationId, best, references));
        }
        if (best != null && best.getScore() != null && best.getScore() >= botProperties.getAnswer().getAssistMinScore())
        {
            AiChatResponse aiResponse = aiProxyClient.chat(
                    conversationId,
                    buildAssistSystemPrompt(best),
                    request.getMessage());
            return recordAndReturn(fromAiWithKb(conversationId, aiResponse, best, references));
        }

        AiChatResponse aiResponse = aiProxyClient.chat(
                conversationId,
                botProperties.getAnswer().getFallbackSystemPrompt(),
                request.getMessage());
        return recordAndReturn(fromAi(conversationId, aiResponse, references));
    }

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

    private BotChatResponse recordAndReturn(BotChatResponse response)
    {
        conversationRecordService.recordAssistantMessage(response.getConversationId(), response);
        return response;
    }

    private String buildAssistSystemPrompt(KbSearchItem best)
    {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是企业IT运维智能客服助手。请优先依据以下知识库资料回答用户问题，回答要准确、简洁、可执行；");
        prompt.append("如果资料不足以确定结论，请说明需要补充的信息，不要编造。\n");
        prompt.append("知识库问题：").append(best.getQuestion()).append('\n');
        prompt.append("知识库答案：").append(best.getAnswer());
        return prompt.toString();
    }

    private String resolveConversationId(String conversationId)
    {
        return StringUtils.isBlank(conversationId) ? UUID.randomUUID().toString() : conversationId;
    }

    private void validate(BotChatRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getMessage()))
        {
            throw new IllegalArgumentException("消息内容不能为空");
        }
    }
}
