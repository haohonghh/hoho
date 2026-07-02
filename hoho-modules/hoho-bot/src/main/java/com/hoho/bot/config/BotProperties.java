package com.hoho.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文本机器人配置
 *
 * @author hoho
 */
@Component
@ConfigurationProperties(prefix = "hoho.bot")
public class BotProperties
{
    private final Kb kb = new Kb();

    private final AiProxy aiProxy = new AiProxy();

    private final Answer answer = new Answer();

    public Kb getKb()
    {
        return kb;
    }

    public AiProxy getAiProxy()
    {
        return aiProxy;
    }

    public Answer getAnswer()
    {
        return answer;
    }

    public static class Kb
    {
        private String baseUrl = "http://localhost:9204";

        public String getBaseUrl()
        {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl)
        {
            this.baseUrl = baseUrl;
        }
    }

    public static class AiProxy
    {
        private String baseUrl = "http://localhost:9205";

        public String getBaseUrl()
        {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl)
        {
            this.baseUrl = baseUrl;
        }
    }

    public static class Answer
    {
        private double directMinScore = 0.65D;

        private double assistMinScore = 0.5D;

        private int topK = 3;

        private String fallbackSystemPrompt = "你是企业IT运维智能客服助手。知识库未命中时，请给出简洁、谨慎、可执行的建议，并提醒用户必要时联系人工运维。";

        public double getMinScore()
        {
            return directMinScore;
        }

        public void setMinScore(double minScore)
        {
            this.directMinScore = minScore;
        }

        public double getDirectMinScore()
        {
            return directMinScore;
        }

        public void setDirectMinScore(double directMinScore)
        {
            this.directMinScore = directMinScore;
        }

        public double getAssistMinScore()
        {
            return assistMinScore;
        }

        public void setAssistMinScore(double assistMinScore)
        {
            this.assistMinScore = assistMinScore;
        }

        public int getTopK()
        {
            return topK;
        }

        public void setTopK(int topK)
        {
            this.topK = topK;
        }

        public String getFallbackSystemPrompt()
        {
            return fallbackSystemPrompt;
        }

        public void setFallbackSystemPrompt(String fallbackSystemPrompt)
        {
            this.fallbackSystemPrompt = fallbackSystemPrompt;
        }
    }
}
