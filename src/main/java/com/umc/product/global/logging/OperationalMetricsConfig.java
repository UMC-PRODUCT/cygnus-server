package com.umc.product.global.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration(proxyBeanMethods = false)
public class OperationalMetricsConfig {

    @Bean
    @ConditionalOnMissingBean(OperationalMetrics.class)
    public OperationalMetrics operationalMetrics(MeterRegistry meterRegistry) {
        return new OperationalMetrics(meterRegistry);
    }
}
