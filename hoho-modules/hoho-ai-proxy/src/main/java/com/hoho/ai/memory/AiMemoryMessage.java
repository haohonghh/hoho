package com.hoho.ai.memory;

import java.io.Serializable;

/**
 * Redis中保存的短期记忆消息
 *
 * @author hoho
 */
public class AiMemoryMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String type;

    private String text;

    public AiMemoryMessage()
    {
    }

    public AiMemoryMessage(String type, String text)
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
