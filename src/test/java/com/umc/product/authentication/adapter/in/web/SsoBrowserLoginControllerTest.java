package com.umc.product.authentication.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authentication.application.port.in.command.ManageSsoBrowserLoginUseCase;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;
import com.umc.product.authentication.application.port.in.query.GetSsoBrowserLoginUseCase;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.support.IntegrationTestSupport;

import jakarta.servlet.http.Cookie;

@DisplayName("SsoBrowserLoginController")
class SsoBrowserLoginControllerTest extends IntegrationTestSupport {

    @MockitoBean
    ManageSsoBrowserLoginUseCase manageSsoBrowserLoginUseCase;

    @MockitoBean
    GetSsoBrowserLoginUseCase getSsoBrowserLoginUseCase;

    @Test
    @DisplayName("email/password 브라우저 로그인은 HttpOnly SSO 쿠키를 설정하고 토큰은 응답 본문에 노출하지 않는다")
    void 이메일_브라우저_로그인_쿠키_설정() throws Exception {
        // given
        given(manageSsoBrowserLoginUseCase.loginByEmail(argThat(command ->
            command.email().equals("alice@example.com") && command.rawPassword().equals("Strong-Pw-2026")
        ))).willReturn(SsoBrowserLoginInfo.of(
            1L,
            "sso-login-token",
            Instant.now().plusSeconds(3600)
        ));

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "alice@example.com",
                      "password": "Strong-Pw-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=sso-login-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")));

        then(manageSsoBrowserLoginUseCase).should().loginByEmail(argThat(command ->
            command.email().equals("alice@example.com") && command.rawPassword().equals("Strong-Pw-2026")
        ));
    }

    @Test
    @DisplayName("Kakao SSO 로그인은 provider token으로 SSO 쿠키를 설정하고 서비스 토큰을 노출하지 않는다")
    void 카카오_SSO_로그인_쿠키_설정() throws Exception {
        // given
        given(manageSsoBrowserLoginUseCase.loginByOAuthToken(argThat(command ->
            command.provider() == OAuthProvider.KAKAO && command.token().equals("kakao-id-token")
        ))).willReturn(SsoBrowserOAuthLoginResult.loginSuccess(
            OAuthProvider.KAKAO,
            SsoBrowserLoginInfo.of(1L, "sso-login-token", Instant.now().plusSeconds(3600))
        ));

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idToken": "kakao-id-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=sso-login-token")))
            .andExpect(content().string(not(containsString("accessToken"))))
            .andExpect(content().string(not(containsString("refreshToken"))))
            .andExpect(jsonPath("$.result.provider").value("KAKAO"))
            .andExpect(jsonPath("$.result.memberId").value(1L));
    }

    @Test
    @DisplayName("Google SSO 로그인 신규 회원은 쿠키 없이 OAuth 가입 토큰을 반환한다")
    void 구글_SSO_로그인_신규회원_가입토큰_반환() throws Exception {
        // given
        given(manageSsoBrowserLoginUseCase.loginByOAuthToken(argThat(command ->
            command.provider() == OAuthProvider.GOOGLE && command.token().equals("google-access-token")
        ))).willReturn(SsoBrowserOAuthLoginResult.registerRequired(
            OAuthProvider.GOOGLE,
            "oauth-verification-token"
        ));

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessToken": "google-access-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andExpect(jsonPath("$.result.provider").value("GOOGLE"))
            .andExpect(jsonPath("$.result.code").value("REGISTER_REQUIRED"))
            .andExpect(jsonPath("$.result.oAuthVerificationToken").value("oauth-verification-token"));
    }

    @Test
    @DisplayName("Apple SSO 로그인은 authorization code로 SSO 쿠키를 설정한다")
    void 애플_SSO_로그인_쿠키_설정() throws Exception {
        // given
        given(manageSsoBrowserLoginUseCase.loginByAppleAuthorizationCode(argThat(command ->
            command.authorizationCode().equals("apple-authorization-code")
        ))).willReturn(SsoBrowserOAuthLoginResult.loginSuccess(
            OAuthProvider.APPLE,
            SsoBrowserLoginInfo.of(1L, "sso-login-token", Instant.now().plusSeconds(3600))
        ));

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "authorizationCode": "apple-authorization-code"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=sso-login-token")))
            .andExpect(jsonPath("$.result.provider").value("APPLE"))
            .andExpect(jsonPath("$.result.memberId").value(1L));
    }

    @Test
    @DisplayName("내 브라우저 로그인 조회에서 SSO 쿠키가 없으면 401을 반환하고 UseCase를 호출하지 않는다")
    void 내_브라우저_로그인_조회_쿠키_없음_거부() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/me"))
            .andExpect(status().isUnauthorized());

        then(getSsoBrowserLoginUseCase).should(never()).getLogin(anyString());
    }

    @Test
    @DisplayName("내 브라우저 로그인 조회는 Query UseCase로 SSO 쿠키를 조회한다")
    void 내_브라우저_로그인_조회_QueryUseCase_호출() throws Exception {
        // given
        given(getSsoBrowserLoginUseCase.getLogin("sso-login-token"))
            .willReturn(SsoBrowserLoginInfo.of(1L, "sso-login-token", Instant.now().plusSeconds(3600)));

        // when / then
        mockMvc.perform(get("/api/v1/auth/sso/me")
                .cookie(new Cookie("UMC_SSO_LOGIN", "sso-login-token")))
            .andExpect(status().isOk());

        then(getSsoBrowserLoginUseCase).should().getLogin("sso-login-token");
        then(manageSsoBrowserLoginUseCase).should(never()).loginByEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("브라우저 로그아웃은 SSO 쿠키를 삭제한다")
    void 브라우저_로그아웃_쿠키_삭제() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/logout"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }
}
