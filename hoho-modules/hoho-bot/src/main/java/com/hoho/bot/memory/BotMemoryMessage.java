package com.hoho.bot.memory;

/**
 * Redis中保存的短期记忆消息
 *
 * @author hoho
 */
public class BotMemoryMessage
{
    private String type;

    private String text;

    public BotMemoryMessage()
    {
    }

    public BotMemoryMessage(String type, String text)
    {
        this.type = type;
        this.text = text;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
