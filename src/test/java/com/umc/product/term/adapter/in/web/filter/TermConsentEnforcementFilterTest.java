package com.umc.product.term.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.util.PublicEndpointCollector;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class TermConsentEnforcementFilterTest {

    @Mock
    GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("재동의가 필요한 인증 사용자의 일반 API 요청을 차단한다")
    void 재동의가_필요한_인증_사용자의_일반_API_요청을_차단한다() throws ServletException, IOException {
        // given
        authenticate(100L, false);
        TermConsentEnforcementFilter sut = newFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/member");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TERMS-0012");
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    @Test
    @DisplayName("재동의 상태 조회 API는 차단하지 않는다")
    void 재동의_상태_조회_API는_차단하지_않는다() throws ServletException, IOException {
        // given
        authenticate(100L, false);
        TermConsentEnforcementFilter sut = newFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/terms/consent-status/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    @Test
    @DisplayName("재동의 제출 API는 재동의 필요 상태여도 차단하지 않는다")
    void 재동의_제출_API는_재동의_필요_상태여도_차단하지_않는다() throws ServletException, IOException {
        // given
        authenticate(100L, false);
        TermConsentEnforcementFilter sut = newFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/terms/agreements");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    @Test
    @DisplayName("컨텍스트 경로가 포함되어도 허용 API를 차단하지 않는다")
    void 컨텍스트_경로가_포함되어도_허용_API를_차단하지_않는다() throws ServletException, IOException {
        // given
        authenticate(100L, false);
        TermConsentEnforcementFilter sut = newFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/v1/terms/agreements");
        request.setContextPath("/app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    @Test
    @DisplayName("Public 엔드포인트는 재동의 필요 상태여도 차단하지 않는다")
    void Public_엔드포인트는_재동의_필요_상태여도_차단하지_않는다() throws ServletException, IOException {
        // given
        authenticate(100L, false);
        TermConsentEnforcementFilter sut = newFilter(List.of(
            new PublicEndpointCollector.EndpointMatcher(HttpMethod.GET, "/api/v1/public/**")
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    @Test
    @DisplayName("필수 약관을 모두 동의한 인증 사용자의 요청은 통과한다")
    void 필수_약관을_모두_동의한_인증_사용자의_요청은_통과한다() throws ServletException, IOException {
        // given
        authenticate(100L, true);
        TermConsentEnforcementFilter sut = newFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/member");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
        then(getRequiredTermConsentStatusUseCase).should(never()).getRequiredTermConsentStatus(100L);
    }

    private void authenticate(Long memberId, boolean requiredTermsAgreed) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .requiredTermsAgreed(requiredTermsAgreed)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private TermConsentEnforcementFilter newFilter() {
        return newFilter(List.of());
    }

    private TermConsentEnforcementFilter newFilter(List<PublicEndpointCollector.EndpointMatcher> publicEndpoints) {
        return new TermConsentEnforcementFilter(
            objectMapper,
            publicEndpoints
        );
    }
}
