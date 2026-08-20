package com.umc.product.demoday.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demoday.vote-authorization")
public record DemodayVoteAuthorizationProperties(String signingKey) {
}
