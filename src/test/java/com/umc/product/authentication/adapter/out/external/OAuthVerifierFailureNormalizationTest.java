package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;

@DisplayName("OAuth verifier 예상 밖 실패 정규화")
class OAuthVerifierFailureNormalizationTest {

    private static final String CALLBACK = "https://app.example.com/callback";

    @Test
    @DisplayName("Kakao token 교환의 transport 실패를 공통 검증 실패로 변환한다")
    void kakao_token_교환_transport_실패() {
        RestClient restClient = throwingPostClient();
        KakaoTokenVerifier verifier = kakaoVerifier(restClient, "admin-key", mock(OidcPublicKeyResolver.class));

        assertError(
            () -> verifier.verifyAuthorizationCode("code", CALLBACK),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Kakao userinfo transport 실패를 공통 검증 실패로 변환한다")
    void kakao_userinfo_transport_실패() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.get()).willThrow(new IllegalStateException("transport"));
        KakaoTokenVerifier verifier = kakaoVerifier(restClient, "admin-key", mock(OidcPublicKeyResolver.class));

        assertError(
            () -> verifier.verifyAccessToken("access"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Kakao ID token 파싱 실패를 공통 검증 실패로 변환한다")
    void kakao_id_token_파싱_실패() {
        OidcPublicKeyResolver resolver = mock(OidcPublicKeyResolver.class);
        given(resolver.extractKid("jwt")).willThrow(new IllegalStateException("parser"));
        KakaoTokenVerifier verifier = kakaoVerifier(mock(RestClient.class), "admin-key", resolver);

        assertError(
            () -> verifier.verifyIdToken("jwt"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Kakao 사용자 및 admin 연결 해제 transport 실패를 공통 검증 실패로 변환한다")
    void kakao_unlink_transport_실패() {
        KakaoTokenVerifier verifier = kakaoVerifier(
            throwingPostClient(), "admin-key", mock(OidcPublicKeyResolver.class));

        assertError(
            () -> verifier.unlinkUser("access"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
        assertError(
            () -> verifier.unlinkUserByAdmin("provider-id"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Google ID token 파싱 실패를 공통 검증 실패로 변환한다")
    void google_id_token_파싱_실패() {
        OidcPublicKeyResolver resolver = mock(OidcPublicKeyResolver.class);
        given(resolver.extractKid("jwt")).willThrow(new IllegalStateException("parser"));
        GoogleTokenVerifier verifier = googleVerifier(mock(RestClient.class), resolver);

        assertError(
            () -> verifier.verifyIdToken("jwt"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Google tokeninfo 및 revoke transport 실패를 공통 검증 실패로 변환한다")
    void google_transport_실패() {
        RestClient getClient = mock(RestClient.class);
        given(getClient.get()).willThrow(new IllegalStateException("transport"));
        GoogleTokenVerifier getVerifier = googleVerifier(getClient, mock(OidcPublicKeyResolver.class));
        GoogleTokenVerifier postVerifier = googleVerifier(throwingPostClient(), mock(OidcPublicKeyResolver.class));

        assertError(
            () -> getVerifier.verify("access"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
        assertError(
            () -> postVerifier.revokeToken("refresh"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    @Test
    @DisplayName("Apple token 교환 및 revoke transport 실패를 공통 검증 실패로 변환한다")
    void apple_transport_실패() {
        AppleTokenVerifier verifier = new AppleTokenVerifier(
            appleProperties(),
            throwingPostClient(),
            new ObjectMapper(),
            mock(OidcPublicKeyResolver.class)
        );

        assertError(
            () -> verifier.verifyAuthorizationCode("code", ClientType.IOS),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
        assertError(
            () -> verifier.revokeToken("refresh", "apple-ios-client"),
            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
        );
    }

    private KakaoTokenVerifier kakaoVerifier(
        RestClient restClient,
        String adminKey,
        OidcPublicKeyResolver resolver
    ) {
        return new KakaoTokenVerifier(
            restClient,
            new KakaoOAuthProperties(
                "kakao-client", "", adminKey, List.of(CALLBACK),
                new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
            ),
            resolver
        );
    }

    private GoogleTokenVerifier googleVerifier(RestClient restClient, OidcPublicKeyResolver resolver) {
        return new GoogleTokenVerifier(
            restClient,
            resolver,
            new GoogleOAuthProperties(
                List.of("google-client"),
                new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
            )
        );
    }

    private RestClient throwingPostClient() {
        RestClient restClient = mock(RestClient.class);
        given(restClient.post()).willThrow(new IllegalStateException("transport"));
        return restClient;
    }

    private AppleOAuthProperties appleProperties() {
        return new AppleOAuthProperties(
            "apple-ios-client",
            "apple-web-client",
            "apple-team",
            "apple-key",
            privateKeyPem(),
            new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
        );
    }

    private String privateKeyPem() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            KeyPair keyPair = generator.generateKeyPair();
            String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
            return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertError(Runnable action, AuthenticationErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(AuthenticationDomainException.class,
                exception -> assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
