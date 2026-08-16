package com.umc.product.authentication.application.port.in.command.dto;

import java.util.List;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

public record AuthorizeSsoCommand(
    String clientId,
    String redirectUri,
    String responseType,
    String state,
    String codeChallenge,
    String codeChallengeMethod,
    String rawLoginToken,
    List<String> requestOrigins
) {
    public AuthorizeSsoCommand {
        if (isBlank(clientId)
            || isBlank(redirectUri)
            || isBlank(responseType)
            || isBlank(state)
            || isBlank(rawLoginToken)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
        }
        requestOrigins = copyRequestOrigins(requestOrigins);
    }

    public static AuthorizeSsoCommand of(
        String clientId,
        String redirectUri,
        String responseType,
        String state,
        String codeChallenge,
        String codeChallengeMethod,
        String rawLoginToken
    ) {
        return withRequestOrigins(
            clientId,
            redirectUri,
            responseType,
            state,
            codeChallenge,
            codeChallengeMethod,
            rawLoginToken,
            List.of()
        );
    }

    public static AuthorizeSsoCommand of(
        String clientId,
        String redirectUri,
        String responseType,
        String state,
        String codeChallenge,
        String codeChallengeMethod,
        String rawLoginToken,
        String requestOrigin
    ) {
        return withRequestOrigins(
            clientId,
            redirectUri,
            responseType,
            state,
            codeChallenge,
            codeChallengeMethod,
            rawLoginToken,
            requestOrigin == null ? List.of() : List.of(requestOrigin)
        );
    }

    public static AuthorizeSsoCommand withRequestOrigins(
        String clientId,
        String redirectUri,
        String responseType,
        String state,
        String codeChallenge,
        String codeChallengeMethod,
        String rawLoginToken,
        List<String> requestOrigins
    ) {
        return new AuthorizeSsoCommand(
            clientId,
            redirectUri,
            responseType,
            state,
            codeChallenge,
            codeChallengeMethod,
            rawLoginToken,
            requestOrigins
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> copyRequestOrigins(List<String> requestOrigins) {
        if (requestOrigins == null || requestOrigins.isEmpty()) {
            return List.of();
        }
        return requestOrigins.stream()
            .filter(origin -> origin != null && !origin.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }
}
