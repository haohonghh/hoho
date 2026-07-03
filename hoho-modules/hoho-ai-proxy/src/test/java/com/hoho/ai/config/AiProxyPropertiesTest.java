package com.hoho.ai.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiProxyPropertiesTest
{
    @Test
    void 聊天配置支持默认模型与可选模型清单()
    {
        AiProxyProperties properties = new AiProxyProperties();

        assertEquals("qwen-plus", properties.getChat().getDefaultModel());
        assertEquals(List.of("qwen-plus"), properties.getChat().getAvailableModels());

        properties.getChat().setDefaultModel("qwen-turbo");
        properties.getChat().setAvailableModels(List.of("qwen-plus", "qwen-turbo", "qwen-max"));

        assertEquals("qwen-turbo", properties.getChat().getDefaultModel());
        assertEquals(List.of("qwen-plus", "qwen-turbo", "qwen-max"), properties.getChat().getAvailableModels());
    }

    @Test
    void 聊天配置支持按场景定义模型路由()
    {
        AiProxyProperties properties = new AiProxyProperties();

        properties.getChat().getScenes().put("chat_general", scene("qwen-turbo"));
        properties.getChat().getScenes().put("chat_with_kb", scene("qwen-plus"));

        assertEquals("qwen-turbo", properties.getChat().getScenes().get("chat_general").getModel());
        assertEquals("qwen-plus", properties.getChat().getScenes().get("chat_with_kb").getModel());
    }

    private AiProxyProperties.ChatScene scene(String model)
    {
        AiProxyProperties.ChatScene scene = new AiProxyProperties.ChatScene();
        scene.setModel(model);
        return scene;
    }
}
