package com.hoho.bot.studio;

import java.util.List;
import java.util.Locale;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.model.response.KbSearchItem;
import com.hoho.bot.service.BotService;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Spring AI Alibaba Studio 调试用机器人代理
 *
 * @author hoho
 */
@Component
public class BotStudioAgent extends Agent
{
    public static final String APP_NAME = "hoho_bot";

    private static final int MAX_REFERENCE_SIZE = 3;

    private final BotService botService;

    public BotStudioAgent(BotService botService)
    {
        super(APP_NAME, "Hoho文本机器人调试入口");
        this.botService = botService;
    }

    /**
     * 将 Studio 的单轮消息转发给现有 BotService，避免调试入口绕过知识库路由、会话落库和短期记忆。
     *
     * @param userMessage    Studio 传入的用户消息
     * @param runnableConfig Studio 会话配置，threadId 映射为 conversationId
     * @return Studio 可消费的流式输出
     */
    @Override
    public Flux<NodeOutput> stream(UserMessage userMessage, RunnableConfig runnableConfig)
    {
        BotChatRequest request = new BotChatRequest();
        request.setConversationId(resolveConversationId(runnableConfig));
        request.setMessage(userMessage.getText());

        BotChatResponse response = botService.chat(request);
        AssistantMessage assistantMessage = new AssistantMessage(formatResponse(response));
        return Flux.just(new StreamingOutput<>(assistantMessage, "bot", APP_NAME, new OverAllState()));
    }

    @Override
    protected StateGraph initGraph() throws GraphStateException
    {
        return new StateGraph();
    }

    private String resolveConversationId(RunnableConfig runnableConfig)
    {
        if (runnableConfig == null)
        {
            return null;
        }
        return runnableConfig.threadId().orElse(null);
    }

    private String formatResponse(BotChatResponse response)
    {
        StringBuilder text = new StringBuilder();
        text.append(response == null || StringUtils.isBlank(response.getAnswer()) ? "未获取到机器人回答。" : response.getAnswer());

        if (response == null)
        {
            return text.toString();
        }

        text.append("\n\n---\n");
        text.append("会话编号：").append(response.getConversationId()).append('\n');
        text.append("来源：").append(response.getSource());
        if (response.getScore() != null)
        {
            text.append('\n').append("匹配分数：").append(String.format(Locale.ROOT, "%.4f", response.getScore()));
        }
        appendReferences(text, response.getReferences());
        return text.toString();
    }

    private void appendReferences(StringBuilder text, List<KbSearchItem> references)
    {
        if (references == null || references.isEmpty())
        {
            return;
        }
        text.append('\n').append("参考资料：");
        int size = Math.min(MAX_REFERENCE_SIZE, references.size());
        for (int i = 0; i < size; i++)
        {
            KbSearchItem item = references.get(i);
            text.append('\n')
                    .append(i + 1)
                    .append(". ")
                    .append(StringUtils.isBlank(item.getQuestion()) ? "未命名知识条目" : item.getQuestion());
            if (item.getScore() != null)
            {
                text.append("（").append(String.format(Locale.ROOT, "%.4f", item.getScore())).append("）");
            }
        }
    }
}
