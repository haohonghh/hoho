package com.hoho.bot.domain;

import java.util.Date;

/**
 * 机器人回答反馈
 *
 * @author hoho
 */
public class BotFeedback
{
    private Long id;

    private String conversationId;

    private Long messageId;

    private Long feedbackUserId;

    private String feedbackUserName;

    private String feedbackType;

    private String reason;

    private String suggestion;

    private String status;

    private Long acceptedQaId;

    private Date createTime;

    private Date updateTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public Long getFeedbackUserId()
    {
        return feedbackUserId;
    }

    public void setFeedbackUserId(Long feedbackUserId)
    {
        this.feedbackUserId = feedbackUserId;
    }

    public String getFeedbackUserName()
    {
        return feedbackUserName;
    }

    public void setFeedbackUserName(String feedbackUserName)
    {
        this.feedbackUserName = feedbackUserName;
    }

    public String getFeedbackType()
    {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType)
    {
        this.feedbackType = feedbackType;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getSuggestion()
    {
        return suggestion;
    }

    public void setSuggestion(String suggestion)
    {
        this.suggestion = suggestion;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getAcceptedQaId()
    {
        return acceptedQaId;
    }

    public void setAcceptedQaId(Long acceptedQaId)
    {
        this.acceptedQaId = acceptedQaId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
