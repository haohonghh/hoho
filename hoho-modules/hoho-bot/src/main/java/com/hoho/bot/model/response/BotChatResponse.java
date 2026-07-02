package com.hoho.bot.model.response;

import java.util.List;

/**
 * 文本对话响应
 *
 * @author hoho
 */
public class BotChatResponse
{
    private String conversationId;

    private String answer;

    private String source;

    private Double score;

    private List<KbSearchItem> references;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public List<KbSearchItem> getReferences()
    {
        return references;
    }

    public void setReferences(List<KbSearchItem> references)
    {
        this.references = references;
    }
}
