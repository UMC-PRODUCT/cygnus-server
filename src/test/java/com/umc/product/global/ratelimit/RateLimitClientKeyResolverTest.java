package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.security.MemberPrincipal;

class RateLimitClientKeyResolverTest {

    private final RateLimitClientKeyResolver resolver = new RateLimitClientKeyResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 MemberPrincipal 이 있으면 memberId 기준 key를 사용한다")
    void resolve_authenticated_member_key() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(42L)
            .clientType(ClientType.WEB)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
        );

        RateLimitClientKey clientKey = resolver.resolve(new MockHttpServletRequest());

        assertThat(clientKey.value()).isEqualTo("member:42");
        assertThat(clientKey.authenticated()).isTrue();
        assertThat(clientKey.clientType()).isEqualTo("WEB");
    }

    @Test
    @DisplayName("인증 정보가 없으면 remoteAddr 기준 key를 사용하고 X-Forwarded-For는 직접 신뢰하지 않는다")
    void resolve_anonymous_ip_key_from_remote_addr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        RateLimitClientKey clientKey = resolver.resolve(request);

        assertThat(clientKey.value()).isEqualTo("ip:10.0.0.10");
        assertThat(clientKey.authenticated()).isFalse();
        assertThat(clientKey.clientType()).isEqualTo("ANONYMOUS");
    }
}
