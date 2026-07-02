package com.hoho.bot.controller;

import com.hoho.bot.model.request.BotChatRequest;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.bot.service.BotService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
