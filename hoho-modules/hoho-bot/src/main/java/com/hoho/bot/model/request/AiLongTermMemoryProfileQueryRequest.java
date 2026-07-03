package com.hoho.bot.model.request;

/**
 * AI长期记忆画像查询请求
 *
 * @author hoho
 */
public class AiLongTermMemoryProfileQueryRequest
{
    private Long userId;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }
}
