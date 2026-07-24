package com.umc.product.notification.adapter.out.external.lambda;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.lambda")
public record NotificationLambdaProperties(
    boolean enabled,
    String region,
    String installationFunctionName
) {
}
