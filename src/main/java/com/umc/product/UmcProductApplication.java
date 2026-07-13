package com.umc.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UmcProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmcProductApplication.class, args);
    }
}
