package com.hoho.ai.controller;

import com.hoho.ai.model.response.HealthResponse;
import com.hoho.ai.service.ChatService;
import com.hoho.ai.service.EmbeddingService;
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
@RequestMapping("/ai")
public class HealthController
{
    private final ChatService chatService;

    private final EmbeddingService embeddingService;

    public HealthController(ChatService chatService, EmbeddingService embeddingService)
    {
        this.chatService = chatService;
        this.embeddingService = embeddingService;
    }

    @GetMapping("/health")
    public R<HealthResponse> health()
    {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setChatModel(chatService.modelName());
        response.setEmbeddingModel(embeddingService.modelName());
        return R.ok(response);
    }
}
