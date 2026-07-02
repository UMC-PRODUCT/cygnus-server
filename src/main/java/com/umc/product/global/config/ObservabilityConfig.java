package com.umc.product.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.context.ContextSnapshotFactory;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ContextSnapshotFactory contextSnapshotFactory() {
        return ContextSnapshotFactory.builder().build();
    }
}
