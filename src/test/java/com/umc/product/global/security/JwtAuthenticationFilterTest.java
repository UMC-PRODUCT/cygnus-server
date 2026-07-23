package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final JwtAuthenticationFilter sut = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AccessToken을 한 번만 검증·파싱하고 모든 snapshot claim을 Principal에 전달한다")
    void parseOnceAndBuildPrincipal() throws Exception {
        Instant expiresAt = Instant.parse("2026-07-23T01:00:00Z");
        ParsedAccessToken parsed = new ParsedAccessToken(
            100L,
            List.of("USER"),
            ClientType.WEB,
            ClientContextClaims.empty(),
            false,
            expiresAt
        );
        given(jwtTokenProvider.parseAndValidateAccessToken("access-token")).willReturn(parsed);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        FilterChain filterChain = mock(FilterChain.class);

        sut.doFilter(request, new MockHttpServletResponse(), filterChain);

        UsernamePasswordAuthenticationToken authentication =
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        assertThat(principal.getMemberId()).isEqualTo(100L);
        assertThat(principal.isRequiredTermsAgreed()).isFalse();
        assertThat(principal.getAccessTokenExpiresAt()).isEqualTo(expiresAt);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        then(jwtTokenProvider).should().parseAndValidateAccessToken("access-token");
        then(jwtTokenProvider).shouldHaveNoMoreInteractions();
        then(filterChain).should().doFilter(
            org.mockito.ArgumentMatchers.eq(request),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
