package com.umc.product.notification.adapter.in.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.result-consumer")
public record NotificationResultQueueProperties(
    boolean enabled,
    String region,
    String queueUrl,
    int maxMessages,
    int visibilityTimeoutSeconds,
    int waitTimeSeconds
) {

    public NotificationResultQueueProperties {
        if (maxMessages < 1 || maxMessages > 10) {
            maxMessages = 10;
        }
        if (visibilityTimeoutSeconds < 1) {
            visibilityTimeoutSeconds = 180;
        }
        if (waitTimeSeconds < 1 || waitTimeSeconds > 20) {
            waitTimeSeconds = 10;
        }
    }
}
