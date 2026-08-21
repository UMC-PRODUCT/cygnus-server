package com.umc.product.demoday.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demoday.qr")
public record DemodayQrProperties(
    String baseUrl
) {
}
