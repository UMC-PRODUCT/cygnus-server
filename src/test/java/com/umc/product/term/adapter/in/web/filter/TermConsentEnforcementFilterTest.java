package com.umc.product.term.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.response.ApiErrorResponseWriter;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.config.TermConsentEnforcementProperties;

import jakarta.servlet.ServletException;

class TermConsentEnforcementFilterTest {

    private final OperationalMetrics operationalMetrics = mock(OperationalMetrics.class);
    private final ApiErrorResponseWriter errorResponseWriter = new ApiErrorResponseWriter(new ObjectMapper());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("재동의가 필요한 인증 사용자의 일반 API 요청을 403으로 차단한다")
    void blockGeneralApi() throws ServletException, IOException {
        authenticate(false);

        MockHttpServletResponse response = perform(enabledFilter(), "GET", "/api/v1/member/profile");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).contains("TERMS-0012");
        then(operationalMetrics).should()
            .recordSecurityEvent("terms", "reconsent_enforcement", "blocked_rest");
    }

    @Test
    @DisplayName("약관 조회와 재동의 제출은 정확한 HTTP method와 path에서만 허용한다")
    void allowExactTermFlowEndpoints() throws ServletException, IOException {
        authenticate(false);

        assertPassed(perform(enabledFilter(), "GET", "/api/v1/terms"));
        assertPassed(perform(enabledFilter(), "GET", "/api/v1/terms/consent-status/me"));
        assertPassed(perform(enabledFilter(), "POST", "/api/v1/terms/agreements"));
        assertBlocked(perform(enabledFilter(), "POST", "/api/v1/terms"));
    }

    @Test
    @DisplayName("token renew와 logout 경로만 복구 흐름에서 허용한다")
    void allowTokenRecoveryEndpoints() throws ServletException, IOException {
        authenticate(false);

        assertPassed(perform(enabledFilter(), "POST", "/api/v1/auth/token/renew"));
        assertPassed(perform(enabledFilter(), "POST", "/api/v1/auth/logout"));
        assertPassed(perform(enabledFilter(), "POST", "/api/v1/auth/sso/logout"));
        assertBlocked(perform(enabledFilter(), "GET", "/api/v1/auth/token/renew"));
    }

    @Test
    @DisplayName("본인 탈퇴만 허용하고 관리자 회원 삭제는 차단한다")
    void allowOnlySelfWithdrawal() throws ServletException, IOException {
        authenticate(false);

        assertPassed(perform(enabledFilter(), "DELETE", "/api/v1/member"));
        assertBlocked(perform(enabledFilter(), "DELETE", "/api/v1/member/admin/100"));
        assertBlocked(perform(enabledFilter(), "DELETE", "/api/v1/member/100"));
    }

    @Test
    @DisplayName("Public 쓰기 API는 약관 복구 allowlist가 아니면 차단한다")
    void blockUnrelatedPublicWriteEndpoint() throws ServletException, IOException {
        authenticate(false);

        assertBlocked(perform(enabledFilter(), "POST", "/api/v1/recruiting/applications/anonymous"));
    }

    @Test
    @DisplayName("GraphQL과 WebSocket은 채널 전용 인터셉터로 넘긴다")
    void handOffChannelEndpoints() throws ServletException, IOException {
        authenticate(false);

        assertPassed(perform(enabledFilter(), "POST", "/graphql"));
        assertPassed(perform(enabledFilter(), "GET", "/ws/info"));
        assertPassed(perform(enabledFilter(), "GET", "/docs/asyncapi"));
    }

    @Test
    @DisplayName("운영 상태 확인용 health만 허용하고 다른 actuator endpoint는 차단한다")
    void allowOnlyActuatorHealth() throws ServletException, IOException {
        authenticate(false);

        assertPassed(perform(enabledFilter(), "GET", "/actuator/health"));
        assertPassed(perform(enabledFilter(), "GET", "/actuator/health/liveness"));
        assertBlocked(perform(enabledFilter(), "GET", "/actuator/prometheus"));
    }

    @Test
    @DisplayName("컨텍스트 경로를 제거한 애플리케이션 경로로 allowlist를 검사한다")
    void normalizeContextPath() throws ServletException, IOException {
        authenticate(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/v1/terms/agreements");
        request.setContextPath("/app");

        MockHttpServletResponse response = perform(enabledFilter(), request);

        assertPassed(response);
    }

    @Test
    @DisplayName("feature flag 비활성, 익명, 동의 완료 요청은 필터 체인을 호출한다")
    void passNonEnforcedRequests() throws ServletException, IOException {
        authenticate(false);
        assertPassed(perform(disabledFilter(), "GET", "/api/v1/member/profile"));

        SecurityContextHolder.clearContext();
        assertPassed(perform(enabledFilter(), "GET", "/api/v1/member/profile"));

        authenticate(true);
        assertPassed(perform(enabledFilter(), "GET", "/api/v1/member/profile"));
    }

    private MockHttpServletResponse perform(
        TermConsentEnforcementFilter filter,
        String method,
        String path
    ) throws ServletException, IOException {
        return perform(filter, new MockHttpServletRequest(method, path));
    }

    private MockHttpServletResponse perform(
        TermConsentEnforcementFilter filter,
        MockHttpServletRequest request
    ) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void authenticate(boolean requiredTermsAgreed) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(100L)
            .requiredTermsAgreed(requiredTermsAgreed)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private TermConsentEnforcementFilter enabledFilter() {
        return new TermConsentEnforcementFilter(
            new TermConsentEnforcementProperties(true),
            operationalMetrics,
            errorResponseWriter
        );
    }

    private TermConsentEnforcementFilter disabledFilter() {
        return new TermConsentEnforcementFilter(
            new TermConsentEnforcementProperties(false),
            operationalMetrics,
            errorResponseWriter
        );
    }

    private void assertPassed(MockHttpServletResponse response) {
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertBlocked(MockHttpServletResponse response) {
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).contains("TERMS-0012");
    }
}
