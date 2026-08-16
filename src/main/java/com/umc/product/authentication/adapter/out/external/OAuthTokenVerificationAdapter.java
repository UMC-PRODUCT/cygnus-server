package com.umc.product.authentication.adapter.out.external;

import org.springframework.stereotype.Component;

import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.application.port.out.RevokeOAuthTokenPort;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth Token 검증
 * <p>
 * Provider별 검증 로직을 위임하는 Facade 역할을 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenVerificationAdapter implements VerifyOAuthTokenPort, RevokeOAuthTokenPort {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final AppleOAuthProperties appleOAuthProperties;

    @Override
    public OAuthAttributes verify(OAuthProvider provider, String token) {
        log.info("OAuth 토큰 검증 시작: provider={}", provider);

        return switch (provider) {
            case GOOGLE -> googleTokenVerifier.verify(token);
            case KAKAO -> kakaoTokenVerifier.verify(token);
            // Apple은 일반 verify가 아닌 verifyAppleAuthorizationCode 사용을 권장하지만,
            // ID Token 직접 검증이 필요한 경우를 위해 web Services ID 기준으로 audience를 검증한다.
            case APPLE -> appleTokenVerifier.verifyIdToken(token, appleOAuthProperties.webClientId());
        };
    }

    @Override
    public OAuthAttributes verifyAuthorizationCode(OAuthProvider provider, String authorizationCode,
                                                    String redirectUri) {
        log.info("OAuth Authorization Code 교환 시작: provider={}", provider);

        return switch (provider) {
            case KAKAO -> kakaoTokenVerifier.verifyAuthorizationCode(authorizationCode, redirectUri);
            // Apple은 client_id 분기와 refresh token 반환이 필요하므로 verifyAppleAuthorizationCode를 사용해야 한다.
            // Google은 현재 access token 흐름만 지원한다.
            case APPLE, GOOGLE -> {
                log.warn("Authorization code 흐름을 지원하지 않는 provider: {}", provider);
                throw new AuthenticationDomainException(AuthenticationErrorCode.UNSUPPORTED_OAUTH_FLOW);
            }
        };
    }

    @Override
    public AppleAuthorizationCodeResult verifyAppleAuthorizationCode(String authorizationCode, ClientType clientType) {
        log.info("Apple Authorization Code 교환 시작: clientType={}", clientType);
        return appleTokenVerifier.verifyAuthorizationCode(authorizationCode, clientType);
    }

    @Override
    public void revokeAppleToken(String refreshToken, String clientId) {
        log.info("Apple token revoke 시작: clientId={}", clientId);
        appleTokenVerifier.revokeToken(refreshToken, clientId);
    }

    @Override
    public void revokeKakaoToken(String accessToken) {
        log.info("Kakao 사용자 연결 끊기 시작");
        kakaoTokenVerifier.unlinkUser(accessToken);
    }

    @Override
    public void revokeGoogleToken(String token) {
        log.info("Google token revoke 시작");
        googleTokenVerifier.revokeToken(token);
    }
}
