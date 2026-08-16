package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class RateLimitRouteResolverTest {

    private final RateLimitRouteResolver resolver = new RateLimitRouteResolver();

    @Test
    @DisplayName("Spring MVC best matching pattern 이 있으면 route pattern 을 우선 사용한다")
    void resolve_best_matching_pattern_first() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/products/{productId}");

        assertThat(resolver.resolve(request)).isEqualTo("/api/v1/products/{productId}");
    }

    @Test
    @DisplayName("route pattern 이 없으면 고정된 unmapped fallback 을 사용한다")
    void resolve_unmapped_fallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/1");

        assertThat(resolver.resolve(request)).isEqualTo(RateLimitRouteResolver.UNMAPPED_ROUTE_PATTERN);
    }
}
