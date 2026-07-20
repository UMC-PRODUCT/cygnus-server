package com.umc.product.authentication.adapter.out.external;

import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_속성;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthTokenVerificationAdapter")
class OAuthTokenVerificationAdapterTest {

    @Mock
    GoogleTokenVerifier googleTokenVerifier;
    @Mock
    KakaoTokenVerifier kakaoTokenVerifier;
    @Mock
    AppleTokenVerifier appleTokenVerifier;
    @Mock
    AppleOAuthProperties appleOAuthProperties;

    @InjectMocks
    OAuthTokenVerificationAdapter sut;

    @Test
    @DisplayName("Google token 검증을 Google verifier에 위임한다")
    void Google_token을_검증한다() {
        OAuthAttributes attributes = attributes(OAuthProvider.GOOGLE);
        given(googleTokenVerifier.verify("token")).willReturn(attributes);

        assertThat(sut.verify(OAuthProvider.GOOGLE, "token")).isSameAs(attributes);
    }

    @Test
    @DisplayName("Kakao token 검증을 Kakao verifier에 위임한다")
    void Kakao_token을_검증한다() {
        OAuthAttributes attributes = attributes(OAuthProvider.KAKAO);
        given(kakaoTokenVerifier.verify("token")).willReturn(attributes);

        assertThat(sut.verify(OAuthProvider.KAKAO, "token")).isSameAs(attributes);
    }

    @Test
    @DisplayName("Apple ID token은 web client ID를 audience로 검증한다")
    void Apple_ID_token을_검증한다() {
        OAuthAttributes attributes = attributes(OAuthProvider.APPLE);
        given(appleOAuthProperties.webClientId()).willReturn("web-client-id");
        given(appleTokenVerifier.verifyIdToken("token", "web-client-id")).willReturn(attributes);

        assertThat(sut.verify(OAuthProvider.APPLE, "token")).isSameAs(attributes);
    }

    @Test
    @DisplayName("Kakao authorization code와 redirect URI를 교환한다")
    void Kakao_authorization_code를_검증한다() {
        OAuthAttributes attributes = attributes(OAuthProvider.KAKAO);
        given(kakaoTokenVerifier.verifyAuthorizationCode("code", "https://client/callback"))
            .willReturn(attributes);

        OAuthAttributes result = sut.verifyAuthorizationCode(
            OAuthProvider.KAKAO, "code", "https://client/callback");

        assertThat(result).isSameAs(attributes);
    }

    @Test
    @DisplayName("Apple authorization code는 일반 교환 경로를 거부한다")
    void Apple_일반_authorization_code_경로를_거부한다() {
        assertThatThrownBy(() -> sut.verifyAuthorizationCode(OAuthProvider.APPLE, "code", "redirect"))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("Google authorization code는 지원하지 않는다")
    void Google_authorization_code를_거부한다() {
        assertThatThrownBy(() -> sut.verifyAuthorizationCode(OAuthProvider.GOOGLE, "code", "redirect"))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("Apple authorization code와 client type 교환을 전용 verifier에 위임한다")
    void Apple_authorization_code를_검증한다() {
        AppleAuthorizationCodeResult expected = new AppleAuthorizationCodeResult(
            attributes(OAuthProvider.APPLE), "refresh-token", "client-id");
        given(appleTokenVerifier.verifyAuthorizationCode("code", ClientType.IOS)).willReturn(expected);

        AppleAuthorizationCodeResult result = sut.verifyAppleAuthorizationCode("code", ClientType.IOS);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Apple refresh token 폐기를 전용 verifier에 위임한다")
    void Apple_token을_폐기한다() {
        sut.revokeAppleToken("refresh-token", "client-id");

        then(appleTokenVerifier).should().revokeToken("refresh-token", "client-id");
    }

    @Test
    @DisplayName("Kakao 사용자 연결 해제를 전용 verifier에 위임한다")
    void Kakao_연결을_해제한다() {
        sut.revokeKakaoToken("access-token");

        then(kakaoTokenVerifier).should().unlinkUser("access-token");
    }

    @Test
    @DisplayName("Google token 폐기를 전용 verifier에 위임한다")
    void Google_token을_폐기한다() {
        sut.revokeGoogleToken("token");

        then(googleTokenVerifier).should().revokeToken("token");
    }

    private OAuthAttributes attributes(OAuthProvider provider) {
        return OAuth_속성(provider, "provider-id", "member@example.com");
    }
}
