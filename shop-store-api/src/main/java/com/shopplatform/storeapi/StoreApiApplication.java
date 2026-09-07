package com.shopplatform.storeapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shopplatform")
@MapperScan("com.shopplatform.domain.**.mapper")
public class StoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreApiApplication.class, args);
    }
}
