package com.umc.product.notification.adapter.out.external.lambda;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
@EnableConfigurationProperties(NotificationLambdaProperties.class)
@ConditionalOnProperty(
    prefix = "app.notification.lambda",
    name = "enabled",
    havingValue = "true"
)
public class NotificationLambdaConfig {

    @Bean
    public LambdaClient notificationLambdaClient(NotificationLambdaProperties properties) {
        return LambdaClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
}
