package com.hoho.bot.model.request;

/**
 * 采纳纠错反馈请求
 *
 * @author hoho
 */
public class BotFeedbackAcceptRequest
{
    private Long categoryId;

    private String question;

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }
}
