package com.hoho.bot.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Bot 流式客户端配置
 *
 * @author hoho
 */
@Configuration
public class BotStreamClientConfig
{
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder()
    {
        return WebClient.builder();
    }
}
