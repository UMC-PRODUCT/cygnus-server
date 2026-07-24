package com.umc.product.global.event.adapter.out.eventbridge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
@EnableConfigurationProperties(EventBridgeIntegrationProperties.class)
@ConditionalOnProperty(
    prefix = "app.event-outbox.eventbridge",
    name = "enabled",
    havingValue = "true"
)
public class EventBridgeIntegrationConfig {

    @Bean
    public EventBridgeClient eventBridgeClient(EventBridgeIntegrationProperties properties) {
        return EventBridgeClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
}
