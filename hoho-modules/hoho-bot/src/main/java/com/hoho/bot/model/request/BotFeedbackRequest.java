package com.hoho.bot.model.request;

/**
 * 回答反馈请求
 *
 * @author hoho
 */
public class BotFeedbackRequest
{
    private String conversationId;

    private Long messageId;

    private String feedbackType;

    private String reason;

    private String suggestion;

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
}
