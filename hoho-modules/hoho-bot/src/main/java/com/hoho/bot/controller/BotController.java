package com.hoho.bot.controller;

import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.service.BotService;
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
 * 文本机器人接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/bot")
public class BotController
{
    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    private final BotService botService;

    public BotController(BotService botService)
    {
        this.botService = botService;
    }

    @PostMapping("/chat")
    public R<BotChatResponse> chat(@RequestBody BotChatRequest request)
    {
        try
        {
            return R.ok(botService.chat(request));
        }
        catch (Exception e)
        {
            log.error("文本机器人对话失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody BotChatRequest request)
    {
        SseEmitter emitter = new SseEmitter(0L);
        botService.streamChat(request)
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
        log.error("文本机器人流式对话失败", error);
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
            log.warn("发送文本机器人流式结束事件失败", e);
        }
        emitter.complete();
    }
}
