package com.umc.product.authentication.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.adapter.in.web.dto.request.RenewAccessTokenRequest;
import com.umc.product.authentication.application.port.in.command.AuthorizeSsoUseCase;
import com.umc.product.authentication.application.port.in.command.ExchangeSsoAuthorizationCodeUseCase;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.config.SsoProperties;

@DisplayName("Authentication web 잔여 계약")
class AuthenticationWebResidualTest {

    @Test
    @DisplayName("token 재발급 controller는 command 변환과 응답 매핑을 수행한다")
    void token_재발급을_위임한다() {
        ManageAuthenticationUseCase useCase = mock(ManageAuthenticationUseCase.class);
        TokenAuthenticationController sut = new TokenAuthenticationController(useCase);
        given(useCase.renewAccessToken(org.mockito.ArgumentMatchers.any())).willReturn(NewTokens.builder()
            .accessToken("access")
            .refreshToken("refresh")
            .build());

        var result = sut.renewAccessToken(new RenewAccessTokenRequest("old-refresh"));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        then(useCase).should().renewAccessToken(org.mockito.ArgumentMatchers.argThat(command ->
            command.refreshToken().equals("old-refresh")));
    }

    @Test
    @DisplayName("SSO cookie는 domain을 적용하고 만료 시 max-age를 0으로 제한한다")
    void sso_cookie의_domain과_만료를_처리한다() {
        SsoProperties properties = new SsoProperties(
            URI.create("https://issuer.example"),
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            new SsoProperties.Cookie("sso-login", ".example.com", true, "Lax"),
            Map.of()
        );
        SsoCookieWriter sut = new SsoCookieWriter(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        sut.writeLoginCookie(response, "login-token", Instant.now().minusSeconds(1));

        assertThat(response.getHeader("Set-Cookie"))
            .contains("Domain=.example.com", "Max-Age=0", "SameSite=Lax");
    }

    @Test
    @DisplayName("Referer origin은 scheme·host·port만 보존하고 잘못된 URI를 무시한다")
    void referer_origin을_정규화한다() {
        SsoOAuthController sut = new SsoOAuthController(
            mock(AuthorizeSsoUseCase.class),
            mock(ExchangeSsoAuthorizationCodeUseCase.class)
        );

        assertThat((String) ReflectionTestUtils.invokeMethod(sut, "deriveOriginFromReferer", " ")).isNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(sut, "deriveOriginFromReferer", "/relative"))
            .isNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(
            sut, "deriveOriginFromReferer", "https://app.example.com:8443/path?q=1"))
            .isEqualTo("https://app.example.com:8443");
        assertThat((String) ReflectionTestUtils.invokeMethod(sut, "deriveOriginFromReferer", "://invalid"))
            .isNull();
        List<?> origins = ReflectionTestUtils.invokeMethod(
            sut, "resolveRequestOrigins", "https://app.example.com", "https://app.example.com/path");
        assertThat(origins).hasSize(1);
        assertThat(origins.getFirst()).isEqualTo("https://app.example.com");
    }
}
