package com.smartretail.mbc.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.smartretail.mbc")
@MapperScan(basePackages = "com.smartretail.mbc.**.mapper")
@EnableAsync
@EnableScheduling
public class MembershipBenefitApplication {

    public static void main(String[] args) {
        SpringApplication.run(MembershipBenefitApplication.class, args);
        System.out.println("===============================================");
        System.out.println("  智慧零售会员权益中心启动成功!");
        System.out.println("  服务地址: http://127.0.0.1:8080/api/mbc");
        System.out.println("  API文档: http://127.0.0.1:8080/api/mbc/doc.html");
        System.out.println("===============================================");
    }
}
