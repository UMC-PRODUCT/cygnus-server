package com.umc.product.authentication.adapter.in.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authentication.application.port.in.command.ExchangeSsoAuthorizationCodeUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ExchangeSsoAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoMemberInfo;
import com.umc.product.authentication.application.port.in.command.dto.SsoTokenInfo;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("SsoOAuthController token")
class SsoOAuthControllerTokenTest extends IntegrationTestSupport {

    @MockitoBean
    ExchangeSsoAuthorizationCodeUseCase exchangeSsoAuthorizationCodeUseCase;

    @Test
    @DisplayName("token endpoint는 form-urlencoded 요청을 command로 변환하고 SSO token 응답을 반환한다")
    void token_form_urlencoded_성공() throws Exception {
        // given
        given(exchangeSsoAuthorizationCodeUseCase.exchange(argThat(command ->
            command.grantType().equals("authorization_code")
                && command.code().equals("raw-code")
                && command.clientId().equals("backoffice")
                && command.redirectUri().equals("https://backoffice.university.neordinary.com/auth/callback")
                && command.codeVerifier().equals("code-verifier")
        ))).willReturn(SsoTokenInfo.of(
            "access-token",
            "refresh-token",
            3600L,
            SsoMemberInfo.of(10L, "홍길동", "길동", "gildong@example.com"),
            List.of(OAuthProvider.GOOGLE, OAuthProvider.KAKAO)
        ));

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "raw-code")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback")
                .param("code_verifier", "code-verifier"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").value("access-token"))
            .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.result.expiresIn").value(3600L))
            .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.result.member.id").value(10L))
            .andExpect(jsonPath("$.result.member.name").value("홍길동"))
            .andExpect(jsonPath("$.result.member.nickname").value("길동"))
            .andExpect(jsonPath("$.result.member.email").value("gildong@example.com"))
            .andExpect(jsonPath("$.result.linkedOAuthProviders[*]", containsInAnyOrder("GOOGLE", "KAKAO")));
    }

    @Test
    @DisplayName("token endpoint 필수 form 필드가 없으면 400을 반환하고 UseCase를 호출하지 않는다")
    void token_form_필수값_누락_거부() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "raw-code")
                .param("client_id", "backoffice")
                .param("redirect_uri", "https://backoffice.university.neordinary.com/auth/callback"))
            .andExpect(status().isBadRequest());

        then(exchangeSsoAuthorizationCodeUseCase).should(never())
            .exchange(org.mockito.ArgumentMatchers.any(ExchangeSsoAuthorizationCodeCommand.class));
    }
}
