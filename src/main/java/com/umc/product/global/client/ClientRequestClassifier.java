package com.umc.product.global.client;

import java.net.URI;
import java.util.Locale;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientRequestClassifier {

    private static final String SOURCE_ORIGIN = "origin";
    private static final String SOURCE_TOKEN = "token";

    private final ClientOriginRegistry originRegistry;

    public ClientRequestContext classify(HttpServletRequest request, MemberPrincipal principal) {
        ClientDeviceType deviceType = resolveDeviceType(request, principal);
        ClientContextClaims tokenClaims = principal == null ? ClientContextClaims.empty() : principal.getClientContextClaims();
        if (tokenClaims == null) {
            tokenClaims = ClientContextClaims.empty();
        }
        String origin = resolveOrigin(request);
        ClientContextProperties.Origin registeredOrigin = originRegistry.findByOrigin(origin).orElse(null);

        boolean tokenServiceKnown = isKnown(tokenClaims.serviceType());
        if (registeredOrigin != null && isKnown(registeredOrigin.serviceType())) {
            boolean mismatched = tokenServiceKnown && tokenClaims.serviceType() != registeredOrigin.serviceType();
            return new ClientRequestContext(
                registeredOrigin.serviceType(),
                deviceType,
                registeredOrigin.environment(),
                SOURCE_ORIGIN,
                mismatched
            );
        }

        if (registeredOrigin != null && tokenServiceKnown) {
            return new ClientRequestContext(
                tokenClaims.serviceType(),
                deviceType,
                registeredOrigin.environment(),
                SOURCE_TOKEN,
                false
            );
        }

        if (tokenServiceKnown) {
            return new ClientRequestContext(
                tokenClaims.serviceType(),
                deviceType,
                tokenClaims.environment(),
                SOURCE_TOKEN,
                false
            );
        }

        if (registeredOrigin != null) {
            return new ClientRequestContext(
                ClientServiceType.UNKNOWN,
                deviceType,
                registeredOrigin.environment(),
                SOURCE_ORIGIN,
                false
            );
        }

        return ClientRequestContext.unknown(deviceType);
    }

    private ClientDeviceType resolveDeviceType(HttpServletRequest request, MemberPrincipal principal) {
        ClientType clientType = principal == null ? null : principal.getClientType();
        if (clientType == ClientType.IOS) {
            return ClientDeviceType.IOS;
        }
        if (clientType == ClientType.ANDROID) {
            return ClientDeviceType.ANDROID;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return ClientDeviceType.UNKNOWN;
        }
        String normalizedUserAgent = userAgent.toLowerCase(Locale.ROOT);
        if (normalizedUserAgent.contains("iphone") || normalizedUserAgent.contains("ipad")) {
            return ClientDeviceType.IOS;
        }
        if (normalizedUserAgent.contains("android")) {
            return ClientDeviceType.ANDROID;
        }
        return ClientDeviceType.DESKTOP;
    }

    private String resolveOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        return deriveOriginFromReferer(request.getHeader("Referer"));
    }

    private String deriveOriginFromReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            StringBuilder origin = new StringBuilder()
                .append(uri.getScheme())
                .append("://")
                .append(uri.getHost());
            if (uri.getPort() >= 0) {
                origin.append(':').append(uri.getPort());
            }
            return origin.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isKnown(ClientServiceType serviceType) {
        return serviceType != null && serviceType != ClientServiceType.UNKNOWN;
    }
}
