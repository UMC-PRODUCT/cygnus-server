package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitPolicyResolverTest {

    @Test
    @DisplayName("기본 설정은 POST /graphql에 인증 사용자 기본 정책을 적용한다")
    void resolve_graphql_authenticated_default_policy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        RateLimitPolicy policy = resolver.resolve("POST", "/graphql", "/graphql", true).orElseThrow();

        assertThat(policy.name()).isEqualTo("authenticated-default");
        assertThat(policy.requestsPerSecond()).isEqualTo(20);
        assertThat(policy.requestsPerMinute()).isEqualTo(300);
    }

    @Test
    @DisplayName("기본 설정은 POST /graphql에 익명 사용자 기본 정책을 적용한다")
    void resolve_graphql_anonymous_default_policy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        RateLimitPolicy policy = resolver.resolve("POST", "/graphql", "/graphql", false).orElseThrow();

        assertThat(policy.name()).isEqualTo("anonymous-default");
        assertThat(policy.requestsPerSecond()).isEqualTo(5);
        assertThat(policy.requestsPerMinute()).isEqualTo(60);
    }

    @Test
    @DisplayName("기본 설정은 활성화되어 있고 /api/** GET 요청에는 인증 사용자 기본 정책을 적용한다")
    void resolve_authenticated_default_policy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        RateLimitPolicy policy = resolver.resolve(
            "GET",
            "/api/v1/projects/{projectId}",
            "/api/v1/projects/1",
            true
        ).orElseThrow();

        assertThat(policy.name()).isEqualTo("authenticated-default");
        assertThat(policy.requestsPerSecond()).isEqualTo(20);
        assertThat(policy.requestsPerMinute()).isEqualTo(300);
    }

    @Test
    @DisplayName("기본 설정에는 route override가 없으므로 검색 형태 경로도 기본 정책을 적용한다")
    void resolve_default_policy_without_route_override() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        RateLimitPolicy policy = resolver.resolve(
            "GET",
            "/api/v1/search/{keyword}",
            "/api/v1/search/spring",
            true
        ).orElseThrow();

        assertThat(policy.name()).isEqualTo("authenticated-default");
        assertThat(policy.requestsPerSecond()).isEqualTo(20);
        assertThat(policy.requestsPerMinute()).isEqualTo(300);
    }

    @Test
    @DisplayName("명시된 route policy가 있으면 기본 정책보다 우선 적용한다")
    void resolve_configured_route_policy() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            true,
            List.of("/api/**"),
            ApiRateLimitProperties.defaultExcludedPaths(),
            new ApiRateLimitProperties.Limit(20, 300),
            new ApiRateLimitProperties.Limit(5, 60),
            List.of(new ApiRateLimitProperties.RoutePolicy(
                "custom",
                List.of("/api/v1/custom/**"),
                List.of("GET"),
                new ApiRateLimitProperties.Limit(3, 30),
                new ApiRateLimitProperties.Limit(1, 10)
            )),
            new ApiRateLimitProperties.Cache(100_000, Duration.ofMinutes(10))
        );
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(properties);

        RateLimitPolicy policy = resolver.resolve(
            "GET",
            "/api/v1/custom/{id}",
            "/api/v1/custom/1",
            true
        ).orElseThrow();

        assertThat(policy.name()).isEqualTo("custom");
        assertThat(policy.requestsPerSecond()).isEqualTo(3);
        assertThat(policy.requestsPerMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("OPTIONS와 제외 경로는 rate limit 대상에서 제외한다")
    void exclude_options_and_configured_paths() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        assertThat(resolver.resolve("OPTIONS", "/api/v1/projects", "/api/v1/projects", false)).isEmpty();
        assertThat(resolver.resolve("OPTIONS", "/graphql", "/graphql", false)).isEmpty();
        assertThat(resolver.resolve("GET", "/docs/scalar.html", "/docs/scalar.html", false)).isEmpty();
        assertThat(resolver.resolve("GET", "/actuator/health", "/actuator/health", false)).isEmpty();
    }

    @Test
    @DisplayName("미매핑 API 요청은 request URI 로 적용 범위를 판단하고 unmapped bucket 을 공유한다")
    void resolve_unmapped_api_policy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        RateLimitPolicy policy = resolver.resolve(
            "GET",
            RateLimitRouteResolver.UNMAPPED_ROUTE_PATTERN,
            "/api/v1/unknown/random-path",
            false
        ).orElseThrow();

        assertThat(policy.name()).isEqualTo("anonymous-default");
        assertThat(policy.requestsPerSecond()).isEqualTo(5);
        assertThat(policy.requestsPerMinute()).isEqualTo(60);
    }

    @Test
    @DisplayName("미매핑 제외 경로는 request URI 기준으로 rate limit 대상에서 제외한다")
    void exclude_unmapped_configured_path() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(ApiRateLimitProperties.defaults());

        assertThat(resolver.resolve(
            "GET",
            RateLimitRouteResolver.UNMAPPED_ROUTE_PATTERN,
            "/docs/unknown",
            false
        )).isEmpty();
    }

    @Test
    @DisplayName("게스트 입장 코드 제출 경로는 전역 정책에서 제외되어 전용 인터셉터가 단독으로 관장한다")
    void exclude_demoday_guest_participation_path() {
        List<String> excludePaths = new java.util.ArrayList<>(ApiRateLimitProperties.defaultExcludedPaths());
        excludePaths.add("/api/v1/demoday/polls/*/participations/guest");
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            true,
            List.of("/api/**"),
            excludePaths,
            new ApiRateLimitProperties.Limit(20, 300),
            new ApiRateLimitProperties.Limit(5, 60),
            List.of(),
            new ApiRateLimitProperties.Cache(100_000, Duration.ofMinutes(10))
        );
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(properties);

        assertThat(resolver.resolve(
            "POST",
            "/api/v1/demoday/polls/{pollId}/participations/guest",
            "/api/v1/demoday/polls/42/participations/guest",
            false
        )).isEmpty();
    }

    @Test
    @DisplayName("비활성화 설정이면 /api/** 요청도 정책을 반환하지 않는다")
    void disabled_properties_returns_empty_policy() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
            false,
            List.of("/api/**"),
            ApiRateLimitProperties.defaultExcludedPaths(),
            new ApiRateLimitProperties.Limit(20, 300),
            new ApiRateLimitProperties.Limit(5, 60),
            List.of(),
            new ApiRateLimitProperties.Cache(100_000, Duration.ofMinutes(10))
        );
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(properties);

        assertThat(resolver.resolve("GET", "/api/v1/projects", "/api/v1/projects", true)).isEmpty();
    }
}
