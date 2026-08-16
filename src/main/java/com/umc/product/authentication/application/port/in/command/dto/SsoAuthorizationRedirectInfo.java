package com.umc.product.authentication.application.port.in.command.dto;

import java.nio.charset.StandardCharsets;

import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

public record SsoAuthorizationRedirectInfo(String redirectUri) {

    public SsoAuthorizationRedirectInfo {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_REDIRECT_URI);
        }
    }

    public static SsoAuthorizationRedirectInfo of(String registeredRedirectUri, String rawCode, String state) {
        if (isBlank(registeredRedirectUri) || isBlank(rawCode) || isBlank(state)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
        }
        String redirectUri = UriComponentsBuilder.fromUriString(registeredRedirectUri)
            .queryParam("code", UriUtils.encode(rawCode, StandardCharsets.UTF_8))
            .queryParam("state", UriUtils.encode(state, StandardCharsets.UTF_8))
            .build(true)
            .toUriString();
        return new SsoAuthorizationRedirectInfo(redirectUri);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
