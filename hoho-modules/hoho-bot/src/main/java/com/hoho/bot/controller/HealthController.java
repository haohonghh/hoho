package com.hoho.bot.controller;

import java.util.HashMap;
import java.util.Map;

import com.hoho.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/bot")
public class HealthController
{
    @GetMapping("/health")
    public R<Map<String, String>> health()
    {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "hoho-bot");
        return R.ok(result);
    }
}
