package com.umc.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

import com.umc.product.global.config.FlywayConfig;

@SpringBootApplication
@ConfigurationPropertiesScan
@Import(FlywayConfig.class)
public class UmcProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmcProductApplication.class, args);
    }
}
