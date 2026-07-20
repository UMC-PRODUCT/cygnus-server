package com.umc.product.authentication.adapter.in.web;

import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_속성;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.adapter.in.web.dto.request.AppleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.GoogleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoCodeLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.OAuthLoginResponse;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizationCodeLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController")
class AuthenticationControllerUnitTest {

    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    ManageAuthenticationUseCase manageAuthenticationUseCase;
    @Mock
    VerifyOAuthTokenPort verifyOAuthTokenPort;
    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    AuthenticationController sut;

    @Test
    @DisplayName("Google 기존 회원은 ID token을 우선 사용하고 클라이언트 정보가 반영된 JWT를 받는다")
    void Google_기존_회원_로그인() {
        OAuthTokenLoginResult loginResult = existing(OAuthProvider.GOOGLE);
        given(oAuthAuthenticationUseCase.accessTokenLogin(
            new AccessTokenLoginCommand(OAuthProvider.GOOGLE, "id-token")))
            .willReturn(loginResult);
        given(manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(1L, ClientType.WEB)))
            .willReturn(tokens());

        OAuthLoginResponse result = sut.googleOAuthLogin(
            new GoogleLoginRequest("id-token", "legacy-token", ClientType.WEB));

        assertThat(result.success()).isTrue();
        assertThat(result.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.oAuthVerificationToken()).isNull();
    }

    @Test
    @DisplayName("Kakao 신규 회원은 legacy access token을 검증하고 회원가입 검증 토큰을 받는다")
    void Kakao_신규_회원_로그인() {
        OAuthTokenLoginResult loginResult = newMember(OAuthProvider.KAKAO);
        given(oAuthAuthenticationUseCase.accessTokenLogin(
            new AccessTokenLoginCommand(OAuthProvider.KAKAO, "access-token")))
            .willReturn(loginResult);
        given(jwtTokenProvider.createOAuthVerificationToken(
            "member@example.com", OAuthProvider.KAKAO, "provider-id"))
            .willReturn("verification-token");

        OAuthLoginResponse result = sut.kakaoOAuthLogin(
            new KakaoLoginRequest(null, "access-token", null));

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("REGISTER_REQUIRED");
        assertThat(result.oAuthVerificationToken()).isEqualTo("verification-token");
        assertThat(result.accessToken()).isNull();
        then(manageAuthenticationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Kakao authorization code 로그인은 code와 redirect URI를 그대로 전달한다")
    void Kakao_authorization_code_로그인() {
        AuthorizationCodeLoginCommand command = new AuthorizationCodeLoginCommand(
            OAuthProvider.KAKAO, "authorization-code", "https://client/callback");
        given(oAuthAuthenticationUseCase.authorizationCodeLogin(command))
            .willReturn(existing(OAuthProvider.KAKAO));
        given(manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(1L, ClientType.ANDROID)))
            .willReturn(tokens());

        OAuthLoginResponse result = sut.kakaoOAuthCodeLogin(
            new KakaoCodeLoginRequest("authorization-code", "https://client/callback", ClientType.ANDROID));

        assertThat(result.success()).isTrue();
        assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("Apple 기존 회원은 refresh token과 client ID를 갱신한 뒤 로그인한다")
    void Apple_기존_회원_refresh_token_갱신() {
        AppleAuthorizationCodeResult codeResult = appleCodeResult("apple-refresh-token");
        given(verifyOAuthTokenPort.verifyAppleAuthorizationCode("authorization-code", ClientType.IOS))
            .willReturn(codeResult);
        given(oAuthAuthenticationUseCase.loginWithOAuthAttributes(codeResult.attrs()))
            .willReturn(existing(OAuthProvider.APPLE));
        given(manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(1L, ClientType.IOS)))
            .willReturn(tokens());

        OAuthLoginResponse result = sut.appleOAuthLogin(
            new AppleLoginRequest("authorization-code", ClientType.IOS));

        assertThat(result.success()).isTrue();
        then(oAuthAuthenticationUseCase).should().updateAppleRefreshToken(
            OAuthProvider.APPLE, "provider-id", "apple-refresh-token", "apple-client-id");
    }

    @Test
    @DisplayName("Apple 기존 회원의 code 교환 결과에 refresh token이 없으면 저장 값을 덮어쓰지 않는다")
    void Apple_refresh_token_없는_기존_회원() {
        AppleAuthorizationCodeResult codeResult = appleCodeResult(null);
        given(verifyOAuthTokenPort.verifyAppleAuthorizationCode("authorization-code", ClientType.WEB))
            .willReturn(codeResult);
        given(oAuthAuthenticationUseCase.loginWithOAuthAttributes(codeResult.attrs()))
            .willReturn(existing(OAuthProvider.APPLE));
        given(manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(1L, ClientType.WEB)))
            .willReturn(tokens());

        OAuthLoginResponse result = sut.appleOAuthLogin(
            new AppleLoginRequest("authorization-code", ClientType.WEB));

        assertThat(result.success()).isTrue();
        then(oAuthAuthenticationUseCase).should(never()).updateAppleRefreshToken(
            eq(OAuthProvider.APPLE), eq("provider-id"), eq(null), eq("apple-client-id"));
    }

    @Test
    @DisplayName("Apple 신규 회원은 refresh token 저장 없이 회원가입 검증 토큰을 받는다")
    void Apple_신규_회원() {
        AppleAuthorizationCodeResult codeResult = appleCodeResult("apple-refresh-token");
        given(verifyOAuthTokenPort.verifyAppleAuthorizationCode("authorization-code", ClientType.IOS))
            .willReturn(codeResult);
        given(oAuthAuthenticationUseCase.loginWithOAuthAttributes(codeResult.attrs()))
            .willReturn(newMember(OAuthProvider.APPLE));
        given(jwtTokenProvider.createOAuthVerificationToken(
            "member@example.com", OAuthProvider.APPLE, "provider-id"))
            .willReturn("verification-token");

        OAuthLoginResponse result = sut.appleOAuthLogin(
            new AppleLoginRequest("authorization-code", ClientType.IOS));

        assertThat(result.oAuthVerificationToken()).isEqualTo("verification-token");
        then(oAuthAuthenticationUseCase).should(never()).updateAppleRefreshToken(
            eq(OAuthProvider.APPLE), eq("provider-id"), eq("apple-refresh-token"), eq("apple-client-id"));
    }

    private OAuthTokenLoginResult existing(OAuthProvider provider) {
        return OAuthTokenLoginResult.existingMember(1L, provider, "provider-id", "member@example.com");
    }

    private OAuthTokenLoginResult newMember(OAuthProvider provider) {
        return OAuthTokenLoginResult.newMember(provider, "provider-id", "member@example.com");
    }

    private NewTokens tokens() {
        return NewTokens.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .build();
    }

    private AppleAuthorizationCodeResult appleCodeResult(String refreshToken) {
        return new AppleAuthorizationCodeResult(
            OAuth_속성(OAuthProvider.APPLE, "provider-id", "member@example.com"),
            refreshToken,
            "apple-client-id"
        );
    }
}
