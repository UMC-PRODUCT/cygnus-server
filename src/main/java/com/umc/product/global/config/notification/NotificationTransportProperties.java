package com.umc.product.global.config.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationTransportProperties(
    NotificationTransport transport
) {

    public NotificationTransportProperties {
        if (transport == null) {
            transport = NotificationTransport.LOCAL;
        }
    }

    public static NotificationTransportProperties local() {
        return new NotificationTransportProperties(NotificationTransport.LOCAL);
    }
}
