package com.hoho.ai.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.ai.model.request.MemoryAppendRequest;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * 短期记忆服务
 *
 * @author hoho
 */
@Service
public class MemoryService
{
    private final ChatMemory chatMemory;

    public MemoryService(ChatMemory chatMemory)
    {
        this.chatMemory = chatMemory;
    }

    /**
     * 追加一轮外部产生的对话消息。
     *
     * <p>该方法用于知识库直接命中等未经过模型调用的路径，确保下一轮模型调用时
     * 仍能通过 Spring AI Advisor 读取到完整的 user/assistant 角色消息。</p>
     *
     * @param request 记忆追加请求
     */
    public void append(MemoryAppendRequest request)
    {
        String conversationId = requireText(request == null ? null : request.getConversationId(), "会话编号不能为空");
        List<Message> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getUserMessage()))
        {
            messages.add(new UserMessage(request.getUserMessage().trim()));
        }
        if (StringUtils.isNotBlank(request.getAssistantMessage()))
        {
            messages.add(new AssistantMessage(request.getAssistantMessage().trim()));
        }
        if (messages.isEmpty())
        {
            throw new IllegalArgumentException("记忆消息不能为空");
        }
        chatMemory.add(conversationId, messages);
    }

    private String requireText(String value, String message)
    {
        if (StringUtils.isBlank(value))
        {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
