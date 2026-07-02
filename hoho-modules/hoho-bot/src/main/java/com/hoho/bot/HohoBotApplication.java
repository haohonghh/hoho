package com.hoho.bot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文本客服服务
 *
 * @author hoho
 */
@MapperScan("com.hoho.bot.mapper")
@SpringBootApplication
public class HohoBotApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(HohoBotApplication.class, args);
        System.out.println("hoho-bot 文本客服服务启动成功");
    }
}
