package com.hoho.kb;

import com.hoho.common.security.annotation.EnableRyFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 知识库服务
 *
 * @author hoho
 */
@EnableRyFeignClients
@MapperScan("com.hoho.kb.mapper")
@SpringBootApplication
public class HohoKbApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(HohoKbApplication.class, args);
        System.out.println("hoho-kb 知识库服务启动成功");
    }
}
