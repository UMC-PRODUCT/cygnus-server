package com.umc.product.authentication.adapter.in.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authentication.adapter.in.web.dto.request.SsoTokenRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.SsoTokenResponse;
import com.umc.product.authentication.application.port.in.command.AuthorizeSsoUseCase;
import com.umc.product.authentication.application.port.in.command.ExchangeSsoAuthorizationCodeUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizeSsoCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/sso/oauth")
@Tag(name = "Authentication | SSO OAuth", description = "SSO OAuth Authorization Code Flow를 처리합니다.")
public class SsoOAuthController {

    private final AuthorizeSsoUseCase authorizeSsoUseCase;
    private final ExchangeSsoAuthorizationCodeUseCase exchangeSsoAuthorizationCodeUseCase;

    @Public
    @GetMapping("/authorize")
    @Operation(operationId = "SSO-OAUTH-001", summary = "SSO OAuth authorization code 발급")
    public ResponseEntity<Void> authorize(
        @RequestParam("client_id") String clientId,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam("response_type") String responseType,
        @RequestParam("state") String state,
        @RequestParam("code_challenge") String codeChallenge,
        @RequestParam("code_challenge_method") String codeChallengeMethod,
        @CookieValue(name = "${app.sso.cookie.name}", required = false) String rawLoginToken,
        @RequestHeader(value = "Origin", required = false) String origin,
        @RequestHeader(value = "Referer", required = false) String referer
    ) {
        if (!StringUtils.hasText(rawLoginToken)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.SSO_BROWSER_LOGIN_REQUIRED);
        }

        SsoAuthorizationRedirectInfo redirectInfo = authorizeSsoUseCase.authorize(AuthorizeSsoCommand.withRequestOrigins(
            clientId,
            redirectUri,
            responseType,
            state,
            codeChallenge,
            codeChallengeMethod,
            rawLoginToken,
            resolveRequestOrigins(origin, referer)
        ));

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectInfo.redirectUri()))
            .build();
    }

    @Public
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(operationId = "SSO-OAUTH-002", summary = "SSO OAuth token 교환")
    public SsoTokenResponse token(@Valid SsoTokenRequest request) {
        return SsoTokenResponse.from(exchangeSsoAuthorizationCodeUseCase.exchange(request.toCommand()));
    }

    private List<String> resolveRequestOrigins(String origin, String referer) {
        List<String> requestOrigins = new ArrayList<>();
        if (StringUtils.hasText(origin)) {
            requestOrigins.add(origin);
        }
        String refererOrigin = deriveOriginFromReferer(referer);
        if (StringUtils.hasText(refererOrigin)) {
            requestOrigins.add(refererOrigin);
        }
        return requestOrigins.stream()
            .distinct()
            .toList();
    }

    private String deriveOriginFromReferer(String referer) {
        if (!StringUtils.hasText(referer)) {
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
}
