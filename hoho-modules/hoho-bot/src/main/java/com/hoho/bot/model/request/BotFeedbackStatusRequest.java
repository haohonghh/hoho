package com.hoho.bot.model.request;

/**
 * 回答反馈状态更新请求
 *
 * @author hoho
 */
public class BotFeedbackStatusRequest
{
    private String status;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
