package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.response.ApiErrorResponseWriter;

class RateLimitResidualTest {

    @Test
    @DisplayName("null 설정은 안전한 기본값으로 정규화한다")
    void properties_null_defaults() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.includePaths()).contains("/api/**", "/graphql");
        assertThat(properties.authenticatedDefault()).isEqualTo(new ApiRateLimitProperties.Limit(20, 300));
        assertThat(properties.anonymousDefault()).isEqualTo(new ApiRateLimitProperties.Limit(5, 60));
        assertThat(properties.routePolicies()).isEmpty();
        assertThat(properties.cache().maximumSize()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("빈 path, 낮은 limit, 불완전 route/cache 설정을 최소 안전값으로 보정한다")
    void properties_nested_value_normalization() {
        ApiRateLimitProperties.Limit limit = new ApiRateLimitProperties.Limit(0, 0);
        ApiRateLimitProperties.RoutePolicy route = new ApiRateLimitProperties.RoutePolicy(
            " ",
            null,
            Arrays.asList(null, " ", " get "),
            null,
            null
        );
        ApiRateLimitProperties.Cache nullTtl = new ApiRateLimitProperties.Cache(0, null);
        ApiRateLimitProperties.Cache negativeTtl = new ApiRateLimitProperties.Cache(1, Duration.ofSeconds(-1));
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            true,
            List.of(),
            List.of(),
            limit,
            limit,
            List.of(route),
            nullTtl
        );

        assertThat(properties.includePaths()).contains("/api/**");
        assertThat(properties.excludePaths()).contains("/actuator/**");
        assertThat(limit.requestsPerSecond()).isOne();
        assertThat(limit.requestsPerMinute()).isOne();
        assertThat(route.name()).isEqualTo("custom");
        assertThat(route.pathPatterns()).isEmpty();
        assertThat(route.methods()).containsExactly("GET");
        assertThat(nullTtl.maximumSize()).isOne();
        assertThat(nullTtl.expireAfterAccess()).isEqualTo(Duration.ofMinutes(10));
        assertThat(negativeTtl.expireAfterAccess()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("method 제한이 없는 custom 정책은 익명 기본 limit fallback을 사용한다")
    void route_policy_anonymous_fallback_and_blank_method() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            true,
            List.of("/api/**"),
            List.of(),
            new ApiRateLimitProperties.Limit(20, 300),
            new ApiRateLimitProperties.Limit(5, 60),
            List.of(new ApiRateLimitProperties.RoutePolicy(
                "fallback",
                List.of("/api/v1/custom/**"),
                List.of(),
                null,
                null
            )),
            new ApiRateLimitProperties.Cache(100, Duration.ofMinutes(1))
        );
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(properties);

        RateLimitPolicy policy = resolver.resolve(
            " ",
            "/api/v1/custom/{id}",
            "/api/v1/custom/1",
            false
        ).orElseThrow();

        assertThat(policy.name()).isEqualTo("fallback");
        assertThat(policy.requestsPerSecond()).isEqualTo(5);
        assertThat(resolver.resolve(
            null,
            RateLimitRouteResolver.UNMAPPED_ROUTE_PATTERN,
            null,
            false
        )).isEmpty();
    }

    @Test
    @DisplayName("SHA-256을 제공하지 않는 비정상 JVM에서도 rate-limit 로그 key를 안전하게 대체한다")
    void hash_key_algorithm_unavailable() throws Exception {
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(
            mock(RateLimitClientKeyResolver.class),
            mock(RateLimitRouteResolver.class),
            mock(RateLimitPolicyResolver.class),
            mock(RateLimitBucketRegistry.class),
            mock(ApiErrorResponseWriter.class),
            mock(ApiRateLimitMetrics.class)
        );

        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                .thenThrow(new NoSuchAlgorithmException("missing"));

            String hash = ReflectionTestUtils.invokeMethod(interceptor, "hashKey", "secret-key");

            assertThat(hash).isEqualTo("sha256-unavailable");
        }
    }
}
