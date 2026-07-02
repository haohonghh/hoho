package com.hoho.ai.model.response;

/**
 * 对话响应
 *
 * @author hoho
 */
public class AiChatResponse
{
    private String conversationId;

    private String content;

    private String model;

    private Usage usage;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public Usage getUsage()
    {
        return usage;
    }

    public void setUsage(Usage usage)
    {
        this.usage = usage;
    }

    public static class Usage
    {
        private Integer inputTokens;

        private Integer outputTokens;

        private Integer totalTokens;

        public Integer getInputTokens()
        {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens)
        {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens()
        {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens)
        {
            this.outputTokens = outputTokens;
        }

        public Integer getTotalTokens()
        {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens)
        {
            this.totalTokens = totalTokens;
        }
    }
}
