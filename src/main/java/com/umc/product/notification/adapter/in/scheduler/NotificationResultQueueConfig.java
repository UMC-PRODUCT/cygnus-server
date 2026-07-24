package com.umc.product.notification.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(NotificationResultQueueProperties.class)
@ConditionalOnProperty(
    prefix = "app.notification.result-consumer",
    name = "enabled",
    havingValue = "true"
)
public class NotificationResultQueueConfig {

    @Bean
    public SqsClient notificationResultSqsClient(NotificationResultQueueProperties properties) {
        return SqsClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
}
