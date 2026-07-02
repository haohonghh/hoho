package com.hoho.bot.model.request;

/**
 * 知识库问答保存请求
 *
 * @author hoho
 */
public class KbQaRequest
{
    private Long categoryId;

    private String question;

    private String answer;

    private String similarQuestions;

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

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getSimilarQuestions()
    {
        return similarQuestions;
    }

    public void setSimilarQuestions(String similarQuestions)
    {
        this.similarQuestions = similarQuestions;
    }
}
