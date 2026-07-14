package com.umc.product.authentication.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.common.domain.enums.OAuthProvider;

/**
 * Authorization Code 기반 OAuth 로그인 Command.
 * <p>
 * 표준 OAuth2 authorization code grant 흐름에서 클라이언트가 받은 code와
 * 인가 요청에 사용한 redirect URI를 함께 전달받습니다.
 * <p>
 * Apple은 클라이언트 플랫폼별 client_id 분기와 refresh token 영속화가 필요하므로
 * 별도의 {@link com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult} 경로를 사용합니다.
 *
 * @param provider          OAuth Provider (현재는 KAKAO만 지원)
 * @param authorizationCode 발급받은 authorization code
 * @param redirectUri       인가 요청에 사용한 redirect URI (token 교환 시 동일 값이어야 함)
 */
public record AuthorizationCodeLoginCommand(
    OAuthProvider provider,
    String authorizationCode,
    String redirectUri
) {
    public AuthorizationCodeLoginCommand {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(authorizationCode, "authorizationCode must not be null");
        Objects.requireNonNull(redirectUri, "redirectUri must not be null");
    }

    public static AuthorizationCodeLoginCommand of(
        OAuthProvider provider,
        String authorizationCode,
        String redirectUri
    ) {
        return new AuthorizationCodeLoginCommand(provider, authorizationCode, redirectUri);
    }
}
