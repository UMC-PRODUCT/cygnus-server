package com.umc.product.authentication.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.common.domain.enums.OAuthProvider;

/**
 * OAuth 토큰 기반 로그인 Command
 * <p>
 * 모바일 클라이언트에서 직접 OAuth 로그인 후 받은 토큰으로 인증할 때 사용합니다.
 * Google/Kakao는 OIDC ID Token을 우선 사용하며, legacy Access Token은 provider adapter에서 fallback 처리합니다.
 *
 * @param provider OAuth Provider
 * @param token    검증할 토큰
 */
public record AccessTokenLoginCommand(
    OAuthProvider provider,
    String token
) {
    public AccessTokenLoginCommand {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(token, "token must not be null");
    }

    public static AccessTokenLoginCommand of(OAuthProvider provider, String token) {
        return new AccessTokenLoginCommand(provider, token);
    }
}
