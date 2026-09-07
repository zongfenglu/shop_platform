package com.shopplatform.app;

import com.shopplatform.adminapi.AdminApiApplication;
import com.shopplatform.clientapi.ClientApiApplication;
import com.shopplatform.job.JobApplication;
import com.shopplatform.mp.MpApplication;
import com.shopplatform.storeapi.StoreApiApplication;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 单进程入口：把五个 Spring Boot 模块的 Controller / Job 扫进来，鉴权仍按路径隔离
 * （/admin /store /api）。本地 compose 默认跑这个，少四个 JVM + 一套 RocketMQ。
 */
@SpringBootApplication(
        scanBasePackages = "com.shopplatform",
        exclude = RocketMQAutoConfiguration.class)
@ComponentScan(
        basePackages = "com.shopplatform",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        AdminApiApplication.class,
                        StoreApiApplication.class,
                        ClientApiApplication.class,
                        JobApplication.class,
                        MpApplication.class,
                        ShopAppApplication.class
                }))
@MapperScan("com.shopplatform.domain.**.mapper")
@EnableScheduling
public class ShopAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopAppApplication.class, args);
    }
}
