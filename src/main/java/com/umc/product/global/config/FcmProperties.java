package com.umc.product.global.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fcm")
public record FcmProperties(
    boolean enabled,
    boolean tokenValidationEnabled
) {
}
