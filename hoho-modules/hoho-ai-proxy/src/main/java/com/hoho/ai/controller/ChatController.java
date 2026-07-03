package com.hoho.ai.controller;

import com.hoho.ai.model.request.ChatRequest;
import com.hoho.ai.model.response.AiChatResponse;
import com.hoho.ai.service.ChatService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request)
    {
        SseEmitter emitter = new SseEmitter(0L);
        chatService.stream(request)
                .subscribe(chunk -> sendChunk(emitter, chunk),
                        error -> completeWithError(emitter, error),
                        () -> complete(emitter));
        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String chunk)
    {
        try
        {
            emitter.send(SseEmitter.event().name("message").data(chunk));
        }
        catch (Exception e)
        {
            completeWithError(emitter, e);
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error)
    {
        log.error("调用流式对话模型失败", error);
        emitter.completeWithError(error);
    }

    private void complete(SseEmitter emitter)
    {
        try
        {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        }
        catch (Exception e)
        {
            log.warn("发送流式结束事件失败", e);
        }
        emitter.complete();
    }
}
