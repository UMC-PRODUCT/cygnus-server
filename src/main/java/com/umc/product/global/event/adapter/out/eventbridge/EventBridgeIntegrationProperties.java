package com.umc.product.global.event.adapter.out.eventbridge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.event-outbox.eventbridge")
public record EventBridgeIntegrationProperties(
    boolean enabled,
    String region,
    String eventBusName
) {
}
