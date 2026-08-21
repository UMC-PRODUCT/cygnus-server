package com.umc.product.demoday.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demoday.stamp-credential")
public record DemodayStampCredentialProperties(
    String encryptionKey
) {
}
