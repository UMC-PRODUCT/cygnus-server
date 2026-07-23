package com.umc.product.term.adapter.in.web.filter;

import static org.mockito.BDDMockito.willReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.security.ParsedAccessToken;
import com.umc.product.support.IntegrationTestSupport;

@TestPropertySource(properties = "app.terms.reconsent.enabled=true")
class TermConsentEnforcementFilterIntegrationTest extends IntegrationTestSupport {

    private static final String TOKEN = "not-agreed-token";
    private static final String AGREED_TOKEN = "agreed-token";

    @BeforeEach
    void setUpToken() {
        willReturn(new ParsedAccessToken(
            100L,
            List.of("USER"),
            null,
            ClientContextClaims.empty(),
            false,
            Instant.now().plusSeconds(3600)
        )).given(jwtTokenProvider).parseAndValidateAccessToken(TOKEN);
        willReturn(new ParsedAccessToken(
            100L,
            List.of("USER"),
            null,
            ClientContextClaims.empty(),
            true,
            Instant.now().plusSeconds(3600)
        )).given(jwtTokenProvider).parseAndValidateAccessToken(AGREED_TOKEN);
    }

    @Test
    @DisplayName("실제 Security filter chain에서 미동의 일반 요청을 TERMS-0012로 차단한다")
    void blockGeneralRequestInActualFilterChain() throws Exception {
        mockMvc.perform(get("/api/v1/challenger/me")
                .header("Authorization", "Bearer " + TOKEN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TERMS-0012"));
    }

    @Test
    @DisplayName("실제 Security filter chain에서도 약관 GET은 통과한다")
    void passTermQueryInActualFilterChain() throws Exception {
        mockMvc.perform(get("/api/v1/terms")
                .header("Authorization", "Bearer " + TOKEN))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public 로그인 쓰기와 관리자 회원 삭제는 실제 filter chain에서 차단한다")
    void blockPublicWriteAndAdminDeleteInActualFilterChain() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/email")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TERMS-0012"));

        mockMvc.perform(delete("/api/v1/member/100")
                .header("Authorization", "Bearer " + TOKEN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TERMS-0012"));
    }

    @Test
    @DisplayName("미동의 GraphQL 요청은 실제 interceptor chain에서 TERMS-0012로 응답한다")
    void blockGraphQlInActualInterceptorChain() throws Exception {
        mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"query { __typename }\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.errors[0].extensions.code").value("TERMS-0012"))
            .andExpect(jsonPath("$.errors[0].extensions.httpStatus").value(403));
    }

    @Test
    @DisplayName("익명 및 동의 완료 GraphQL 요청은 실제 resolver chain으로 전달한다")
    void passAnonymousAndAgreedGraphQlRequests() throws Exception {
        String body = "{\"query\":\"query { __typename }\"}";

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.__typename").value("Query"));

        mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + AGREED_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.__typename").value("Query"));
    }
}
