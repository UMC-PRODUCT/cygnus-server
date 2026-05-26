package com.umc.product.global.config;

import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ContextSnapshotFactory contextSnapshotFactory() {
        return ContextSnapshotFactory.builder().build();
    }
}
