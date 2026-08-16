package com.umc.product.authentication.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

@ConfigurationProperties(prefix = "app.sso")
public record SsoProperties(
    URI issuer,
    Duration authorizationCodeTtl,
    Duration browserLoginTtl,
    Cookie cookie,
    Map<String, Client> clients
) {
    public record Cookie(String name, String domain, boolean secure, String sameSite) {
    }

    public record Client(
        String name,
        ClientServiceType serviceType,
        ClientEnvironment environment,
        boolean requirePkce,
        Duration accessTokenTtl,
        List<String> redirectUris,
        List<String> allowedOrigins
    ) {
    }
}
