package com.hoho.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI能力代理服务
 *
 * @author hoho
 */
@MapperScan("com.hoho.ai.mapper")
@SpringBootApplication
public class HohoAiProxyApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(HohoAiProxyApplication.class, args);
        System.out.println("hoho-ai-proxy AI能力代理服务启动成功");
    }
}
