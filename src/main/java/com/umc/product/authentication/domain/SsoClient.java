package com.umc.product.authentication.domain;

import java.time.Duration;
import java.util.List;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

public record SsoClient(
    String clientId,
    String name,
    ClientServiceType serviceType,
    ClientEnvironment environment,
    boolean requirePkce,
    Duration accessTokenTtl,
    List<String> redirectUris,
    List<String> allowedOrigins
) {
    public SsoClient {
        if (clientId == null || clientId.isBlank()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_CLIENT);
        }
        serviceType = serviceType == null ? ClientServiceType.UNKNOWN : serviceType;
        environment = environment == null ? ClientEnvironment.UNKNOWN : environment;
        redirectUris = copyRedirectUris(redirectUris);
        allowedOrigins = copyAllowedOrigins(allowedOrigins);
    }

    public static SsoClient of(
        String clientId,
        String name,
        ClientServiceType serviceType,
        ClientEnvironment environment,
        boolean requirePkce,
        Duration accessTokenTtl,
        List<String> redirectUris,
        List<String> allowedOrigins
    ) {
        return new SsoClient(
            clientId,
            name,
            serviceType,
            environment,
            requirePkce,
            accessTokenTtl,
            redirectUris,
            allowedOrigins
        );
    }

    public boolean allowsRedirectUri(String redirectUri) {
        return redirectUri != null && redirectUris.contains(redirectUri);
    }

    public boolean allowsOrigin(String origin) {
        String normalizedOrigin = normalizeOrigin(origin);
        return normalizedOrigin != null && allowedOrigins.stream()
            .map(SsoClient::normalizeOrigin)
            .anyMatch(normalizedOrigin::equals);
    }

    private static List<String> copyRedirectUris(List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty() || containsBlank(redirectUris)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_REDIRECT_URI);
        }
        return List.copyOf(redirectUris);
    }

    private static List<String> copyAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null) {
            return List.of();
        }
        if (containsBlank(allowedOrigins)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_CLIENT);
        }
        return List.copyOf(allowedOrigins);
    }

    private static boolean containsBlank(List<String> values) {
        return values.stream()
            .anyMatch(value -> value == null || value.isBlank());
    }

    private static String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        String normalized = origin.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
