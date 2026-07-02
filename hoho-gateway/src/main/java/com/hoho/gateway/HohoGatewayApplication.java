package com.hoho.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 网关启动程序
 * 
 * @author hoho
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class HohoGatewayApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(HohoGatewayApplication.class, args);
    }
}
