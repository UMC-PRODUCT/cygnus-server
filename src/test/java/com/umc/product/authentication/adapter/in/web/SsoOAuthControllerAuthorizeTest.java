package com.umc.product.authentication.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authentication.application.port.in.command.AuthorizeSsoUseCase;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;
import com.umc.product.support.IntegrationTestSupport;

import jakarta.servlet.http.Cookie;

@DisplayName("SsoOAuthController authorize")
@TestPropertySource(properties = "app.cors.allowed-origin-patterns=https://backoffice.university.neordinary.com")
class SsoOAuthControllerAuthorizeTest extends IntegrationTestSupport {

    @MockitoBean
    AuthorizeSsoUseCase authorizeSsoUseCase;

    @Test
    @DisplayName("authorize 성공 시 raw 브라우저 로그인 토큰을 command로 전달하고 registered redirect URI로 302 응답한다")
    void authorize_성공_redirect() throws Exception {
        // given
        given(authorizeSsoUseCase.authorize(argThat(command ->
            command.clientId().equals("backoffice")
                && command.redirectUri().equals("https://backoffice.university.neordinary.com/auth/callback")
                && command.responseType().equals("code")
                && command.state().equals("state-123")
                && command.codeChallenge().equals("challenge-123")
                && command.codeChallengeMethod().equals("S256")
                && command.rawLoginToken().equals("sso-login-token")
                && command.requestOrigins().equals(List.of("https://backoffice.university.neordinary.com"))
        ))).willReturn(SsoAuthorizationRedirectInfo.of(
            "https://backoffice.university.neordinary.com/auth/callback",
            "raw-code",
            "state-123"
        ));

        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback")
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", "challenge-123")
                .param("code_challenge_method", "S256")
                .header(HttpHeaders.ORIGIN, "https://backoffice.university.neordinary.com")
                .cookie(new Cookie("UMC_SSO_LOGIN", "sso-login-token")))
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString(
                "https://backoffice.university.neordinary.com/auth/callback"
            )))
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("code=raw-code")))
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("state=state-123")));
    }

    @Test
    @DisplayName("authorize 요청에 Origin이 없으면 Referer에서 origin만 추출해 command로 전달한다")
    void authorize_referer_origin_전달() throws Exception {
        // given
        given(authorizeSsoUseCase.authorize(argThat(command ->
            command.clientId().equals("backoffice")
                && command.requestOrigins().equals(List.of("https://backoffice.university.neordinary.com"))
        ))).willReturn(SsoAuthorizationRedirectInfo.of(
            "https://backoffice.university.neordinary.com/auth/callback",
            "raw-code",
            "state-123"
        ));

        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback")
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", "challenge-123")
                .param("code_challenge_method", "S256")
                .header(HttpHeaders.REFERER, "https://backoffice.university.neordinary.com/projects/1?tab=form")
                .cookie(new Cookie("UMC_SSO_LOGIN", "sso-login-token")))
            .andExpect(status().isFound());
    }

    @Test
    @DisplayName("authorize 요청에 Origin과 Referer가 모두 있으면 두 origin을 모두 command로 전달한다")
    void authorize_origin_referer_모두_전달() throws Exception {
        // given
        given(authorizeSsoUseCase.authorize(argThat(command ->
            command.clientId().equals("backoffice")
                && command.requestOrigins().equals(List.of(
                    "https://backoffice.university.neordinary.com",
                    "https://evil.example.com"
                ))
        ))).willReturn(SsoAuthorizationRedirectInfo.of(
            "https://backoffice.university.neordinary.com/auth/callback",
            "raw-code",
            "state-123"
        ));

        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback")
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", "challenge-123")
                .param("code_challenge_method", "S256")
                .header(HttpHeaders.ORIGIN, "https://backoffice.university.neordinary.com")
                .header(HttpHeaders.REFERER, "https://evil.example.com/projects/1")
                .cookie(new Cookie("UMC_SSO_LOGIN", "sso-login-token")))
            .andExpect(status().isFound());
    }

    @Test
    @DisplayName("authorize 요청에서 SSO 쿠키가 없으면 401을 반환하고 UseCase를 호출하지 않는다")
    void authorize_쿠키_없음_거부() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback")
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", "challenge-123")
                .param("code_challenge_method", "S256"))
            .andExpect(status().isUnauthorized());

        then(authorizeSsoUseCase).shouldHaveNoInteractions();
    }
}
