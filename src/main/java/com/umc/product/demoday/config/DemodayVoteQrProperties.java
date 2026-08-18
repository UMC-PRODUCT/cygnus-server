package com.umc.product.demoday.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demoday.vote-qr")
public record DemodayVoteQrProperties(String signingKey) {
}
