package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.ApiErrorResponseWriter;

class SecurityFilterResidualTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT domain 예외를 request에 보존하고 filter chain은 계속 진행한다")
    void jwt_domain_exception() throws Exception {
        JwtTokenProvider provider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        AuthenticationDomainException error = new AuthenticationDomainException(
            AuthenticationErrorCode.EXPIRED_JWT_TOKEN
        );
        given(provider.validateAccessToken("expired")).willThrow(error);
        MockHttpServletRequest request = bearerRequest("expired");
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthenticationFilter(provider).doFilter(
            request,
            new MockHttpServletResponse(),
            chain
        );

        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE))
            .isSameAs(error);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("예상하지 못한 JWT 예외를 별도 attribute에 보존한다")
    void jwt_unknown_exception() throws Exception {
        JwtTokenProvider provider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        IllegalStateException error = new IllegalStateException("parser failed");
        given(provider.validateAccessToken("broken")).willReturn(true);
        given(provider.parseAccessToken("broken")).willThrow(error);
        MockHttpServletRequest request = bearerRequest("broken");

        new JwtAuthenticationFilter(provider).doFilter(
            request,
            new MockHttpServletResponse(),
            new MockFilterChain()
        );

        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_UNKNOWN_ERROR_ATTRIBUTE))
            .isSameAs(error);
    }

    @Test
    @DisplayName("유효한 JWT의 role을 authority로 변환한다")
    void jwt_roles_to_authorities() throws Exception {
        JwtTokenProvider provider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        given(provider.validateAccessToken("valid")).willReturn(true);
        given(provider.parseAccessToken("valid")).willReturn(7L);
        given(provider.getRolesFromAccessToken("valid")).willReturn(List.of("ADMIN", "MEMBER"));

        new JwtAuthenticationFilter(provider).doFilter(
            bearerRequest("valid"),
            new MockHttpServletResponse(),
            new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_ADMIN", "ROLE_MEMBER");
    }

    @Test
    @DisplayName("AuthenticationEntryPoint는 filter domain 예외를 우선 응답한다")
    void entry_point_filter_domain_error() throws Exception {
        AuthenticationDomainException error = new AuthenticationDomainException(
            AuthenticationErrorCode.EXPIRED_JWT_TOKEN
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, error);

        MockHttpServletResponse response = commence(request, new InsufficientAuthenticationException("denied"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(AuthenticationErrorCode.EXPIRED_JWT_TOKEN.getCode());
    }

    @Test
    @DisplayName("AuthenticationEntryPoint는 알 수 없는 JWT 예외를 내부 오류로 응답한다")
    void entry_point_unknown_error() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setAttribute(
            JwtAuthenticationFilter.JWT_UNKNOWN_ERROR_ATTRIBUTE,
            new IllegalStateException("secret parser detail")
        );

        MockHttpServletResponse response = commence(request, new InsufficientAuthenticationException("denied"));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
            .contains(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .doesNotContain("secret parser detail");
    }

    @Test
    @DisplayName("AuthenticationEntryPoint는 cause의 domain 예외를 fallback으로 사용한다")
    void entry_point_cause_domain_error() throws Exception {
        AuthenticationDomainException cause = new AuthenticationDomainException(
            AuthenticationErrorCode.INVALID_JWT
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");

        MockHttpServletResponse response = commence(
            request,
            new InsufficientAuthenticationException("denied", cause)
        );

        assertThat(response.getContentAsString()).contains(AuthenticationErrorCode.INVALID_JWT.getCode());
    }

    @Test
    @DisplayName("인증 오류 정보가 없으면 SECURITY_NOT_GIVEN을 응답한다")
    void entry_point_default_error() throws Exception {
        MockHttpServletResponse response = commence(
            new MockHttpServletRequest("GET", "/api/v1/members/me"),
            new InsufficientAuthenticationException("internal detail")
        );

        assertThat(response.getContentAsString())
            .contains(CommonErrorCode.SECURITY_NOT_GIVEN.getCode())
            .doesNotContain("internal detail");
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private MockHttpServletResponse commence(
        MockHttpServletRequest request,
        InsufficientAuthenticationException exception
    ) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(
            new ApiErrorResponseWriter(new ObjectMapper())
        );
        entryPoint.commence(request, response, exception);
        return response;
    }
}
