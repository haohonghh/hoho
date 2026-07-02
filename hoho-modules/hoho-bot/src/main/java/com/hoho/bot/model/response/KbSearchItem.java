package com.hoho.bot.model.response;

/**
 * 知识库检索结果
 *
 * @author hoho
 */
public class KbSearchItem
{
    private Long qaId;

    private String question;

    private String answer;

    private Double score;

    private String source;

    public Long getQaId()
    {
        return qaId;
    }

    public void setQaId(Long qaId)
    {
        this.qaId = qaId;
    }

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
