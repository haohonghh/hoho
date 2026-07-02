package com.hoho.kb.domain;

import java.time.LocalDateTime;

/**
 * 问答知识
 *
 * @author hoho
 */
public class Qa
{
    private Long id;

    private Long categoryId;

    private String question;

    private String answer;

    private String similarQuestions;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime)
    {
        this.updateTime = updateTime;
    }
}
