package com.hoho.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * AI能力代理服务
 *
 * @author hoho
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class HohoAiProxyApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(HohoAiProxyApplication.class, args);
        System.out.println("hoho-ai-proxy AI能力代理服务启动成功");
    }
}
