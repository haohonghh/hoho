package com.hoho.ai.controller;

import com.hoho.ai.model.request.ChatRequest;
import com.hoho.ai.model.response.AiChatResponse;
import com.hoho.ai.service.ChatService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/ai")
public class ChatController
{
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService)
    {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public R<AiChatResponse> chat(@RequestBody ChatRequest request)
    {
        try
        {
            return R.ok(chatService.chat(request));
        }
        catch (Exception e)
        {
            log.error("调用对话模型失败", e);
            return R.fail(e.getMessage());
        }
    }
}
