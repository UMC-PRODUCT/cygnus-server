package com.umc.product.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authentication.adapter.in.web.dto.request.ChangePasswordRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.LoginByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.RegisterCredentialRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.RenewAccessTokenRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.ResetPasswordByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.SsoBrowserLoginByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.SsoOAuthTokenLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.RenewAccessTokenResponse;
import com.umc.product.authentication.adapter.out.external.AppleOAuthProperties;
import com.umc.product.authentication.adapter.out.external.GoogleOAuthProperties;
import com.umc.product.authentication.adapter.out.external.KakaoOAuthProperties;
import com.umc.product.authentication.adapter.out.external.OidcJwksCacheProperties;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizeSsoCommand;
import com.umc.product.authentication.application.port.in.command.dto.ChangePasswordCommand;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByAppleAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByOAuthTokenCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.OAuthLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;

@DisplayName("Authentication 값 객체 잔여 계약")
class AuthenticationResidualContractTest {

    private static final String PASSWORD = "Password123!";

    @Test
    @DisplayName("SSO client는 필수 ID와 origin 정규화 규칙을 강제한다")
    void sso_client_방어_계약() {
        assertThatThrownBy(() -> client(" ", List.of()))
            .isInstanceOf(AuthenticationDomainException.class);

        SsoClient withoutOrigins = client("client", null);
        SsoClient normalized = client("client", List.of("https://umc.example"));

        assertThat(withoutOrigins.allowedOrigins()).isEmpty();
        assertThat(normalized.allowsOrigin(null)).isFalse();
        assertThat(normalized.allowsOrigin("   ")).isFalse();
        assertThat(normalized.allowsOrigin("https://umc.example///")).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 OAuth provider registration ID는 도메인 예외로 거부한다")
    void 알_수_없는_oauth_provider를_거부한다() {
        assertThatThrownBy(() -> OAuthAttributes.of("unknown", Map.of()))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("OAuth 설정은 누락·잘못된 cache 값을 안전한 기본값으로 정규화한다")
    void oauth_설정_기본값을_적용한다() {
        OidcJwksCacheProperties invalid = new OidcJwksCacheProperties(Duration.ZERO, 0);
        assertThat(invalid.ttl()).isEqualTo(Duration.ofHours(24));
        assertThat(invalid.maxSize()).isEqualTo(1L);
        assertThat(OidcJwksCacheProperties.defaults()).isEqualTo(invalid);

        assertThat(new GoogleOAuthProperties(null, null).clientIdList()).isEmpty();
        assertThat(new GoogleOAuthProperties(List.of(" ", "client"), null).clientIdList())
            .containsExactly("client");
        assertThat(new KakaoOAuthProperties("id", null, null, null, null).allowedRedirectUris()).isEmpty();
        assertThat(new KakaoOAuthProperties("id", null, null, List.of("uri"), null)
            .isAllowedRedirectUri(null)).isFalse();
        assertThat(new AppleOAuthProperties("ios", "web", null, null, null, null).jwksCache())
            .isEqualTo(OidcJwksCacheProperties.defaults());
    }

    @Test
    @DisplayName("요청 DTO는 command로 변환하면서 비밀번호와 인증 token을 문자열에서 마스킹한다")
    void 요청_dto_변환과_마스킹() {
        SsoBrowserLoginByEmailRequest browser = new SsoBrowserLoginByEmailRequest("a@b.com", PASSWORD);
        RegisterCredentialRequest register = new RegisterCredentialRequest(PASSWORD);
        ChangePasswordRequest change = new ChangePasswordRequest("OldPassword1!", PASSWORD);
        ResetPasswordByEmailRequest reset = new ResetPasswordByEmailRequest("verification-token", PASSWORD);
        LoginByEmailRequest login = new LoginByEmailRequest("a@b.com", PASSWORD, ClientType.WEB);

        assertThat(browser.toCommand().email()).isEqualTo("a@b.com");
        assertThat(register.toCommand(1L).memberId()).isEqualTo(1L);
        assertThat(change.toCommand(1L).newRawPassword()).isEqualTo(PASSWORD);
        assertThat(reset.toCommand("a@b.com").email()).isEqualTo("a@b.com");
        assertThat(login.toCommand().clientType()).isEqualTo(ClientType.WEB);
        assertThat(new RenewAccessTokenRequest("refresh").toCommand().refreshToken()).isEqualTo("refresh");
        assertThat(List.of(browser, register, change, reset, login))
            .allSatisfy(request -> assertThat(request.toString()).doesNotContain(PASSWORD, "verification-token"));
    }

    @Test
    @DisplayName("OAuth token 요청은 둘 중 하나를 요구하고 ID token을 우선한다")
    void oauth_token_요청을_검증한다() {
        assertThatThrownBy(() -> new SsoOAuthTokenLoginRequest(" ", null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(new SsoOAuthTokenLoginRequest("id-token", "access-token")
            .toCommand(OAuthProvider.GOOGLE).token()).isEqualTo("id-token");
    }

    @Test
    @DisplayName("command factory와 민감정보 마스킹 계약을 보존한다")
    void command_factory와_마스킹() {
        assertThat(AuthorizeSsoCommand.of(
            "client", "https://callback", "code", "state", null, null, "login-token").requestOrigins())
            .isEmpty();
        assertThat(LoginByEmailCommand.of("a@b.com", PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(LoginSsoBrowserByEmailCommand.of("a@b.com", PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(ResetPasswordByEmailCommand.of("a@b.com", PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(ChangePasswordCommand.of(1L, "OldPassword1!", PASSWORD).toString())
            .doesNotContain(PASSWORD, "OldPassword1!");
        assertThat(RegisterCredentialByEmailCommand.of(1L, PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(IssueAuthenticationTokensCommand.of(1L).clientType()).isNull();
    }

    @Test
    @DisplayName("필수 OAuth·SSO command 값과 redirect 구성 값을 검증한다")
    void command_필수값을_검증한다() {
        assertThatThrownBy(() -> AuthorizeSsoCommand.of(
            "", "https://callback", "code", "state", null, null, "login-token"))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThatThrownBy(() -> new LoginSsoBrowserByOAuthTokenCommand(null, "token"))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LoginSsoBrowserByOAuthTokenCommand.of(OAuthProvider.GOOGLE, " "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OAuthLoginCommand(null, "id", "a@b.com", null, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OAuthLoginCommand(OAuthProvider.GOOGLE, null, "a@b.com", null, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OAuthLoginCommand(OAuthProvider.GOOGLE, "id", null, null, null))
            .isInstanceOf(NullPointerException.class);
        assertThat(new OAuthLoginCommand(OAuthProvider.GOOGLE, "id", "a@b.com", null, null).email())
            .isEqualTo("a@b.com");
        assertThatThrownBy(() -> LoginSsoBrowserByAppleAuthorizationCodeCommand.from(" "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SsoAuthorizationRedirectInfo(null))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThatThrownBy(() -> SsoAuthorizationRedirectInfo.of("https://callback", "", "state"))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("token 재발급 응답은 발급 결과를 그대로 매핑한다")
    void token_응답을_매핑한다() {
        RenewAccessTokenResponse response = RenewAccessTokenResponse.from(NewTokens.builder()
            .accessToken("access")
            .refreshToken("refresh")
            .build());

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    private SsoClient client(String clientId, List<String> origins) {
        return SsoClient.of(
            clientId,
            "client",
            null,
            null,
            false,
            Duration.ofHours(1),
            List.of("https://callback"),
            origins
        );
    }
}
