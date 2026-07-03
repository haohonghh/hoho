package com.hoho.ai.model.response;

import java.util.List;

/**
 * 短期记忆调试响应
 *
 * @author hoho
 */
public class MemoryDebugResponse
{
    private String conversationId;

    private Integer totalMessages;

    private List<MemoryMessageItem> messages;

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public Integer getTotalMessages()
    {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages)
    {
        this.totalMessages = totalMessages;
    }

    public List<MemoryMessageItem> getMessages()
    {
        return messages;
    }

    public void setMessages(List<MemoryMessageItem> messages)
    {
        this.messages = messages;
    }

    public static class MemoryMessageItem
    {
        private String role;

        private String content;

        public String getRole()
        {
            return role;
        }

        public void setRole(String role)
        {
            this.role = role;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }
    }
}
