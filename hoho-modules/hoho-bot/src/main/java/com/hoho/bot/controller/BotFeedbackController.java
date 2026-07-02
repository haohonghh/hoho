package com.hoho.bot.controller;

import java.util.List;

import com.hoho.bot.domain.BotFeedback;
import com.hoho.bot.model.request.BotFeedbackAcceptRequest;
import com.hoho.bot.model.request.BotFeedbackRequest;
import com.hoho.bot.model.request.BotFeedbackStatusRequest;
import com.hoho.bot.service.BotFeedbackService;
import com.hoho.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回答反馈接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/bot/feedback")
public class BotFeedbackController
{
    private static final Logger log = LoggerFactory.getLogger(BotFeedbackController.class);

    private final BotFeedbackService botFeedbackService;

    public BotFeedbackController(BotFeedbackService botFeedbackService)
    {
        this.botFeedbackService = botFeedbackService;
    }

    @PostMapping
    public R<Void> submit(@RequestBody BotFeedbackRequest request)
    {
        try
        {
            botFeedbackService.submit(request);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("提交回答反馈失败", e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public R<List<BotFeedback>> list(@RequestParam(required = false) String status)
    {
        try
        {
            return R.ok(botFeedbackService.list(status));
        }
        catch (Exception e)
        {
            log.error("查询回答反馈失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody BotFeedbackStatusRequest request)
    {
        try
        {
            botFeedbackService.updateStatus(id, request);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("更新回答反馈状态失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/accept")
    public R<Long> accept(@PathVariable Long id, @RequestBody BotFeedbackAcceptRequest request)
    {
        try
        {
            return R.ok(botFeedbackService.acceptCorrection(id, request));
        }
        catch (Exception e)
        {
            log.error("采纳回答反馈失败", e);
            return R.fail(e.getMessage());
        }
    }
}
