package com.umc.product.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.terms.reconsent", name = "enabled", havingValue = "true")
public class WebSocketSessionExpiryConfig {

    @Bean
    public ThreadPoolTaskScheduler webSocketSessionExpiryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-token-expiry-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
