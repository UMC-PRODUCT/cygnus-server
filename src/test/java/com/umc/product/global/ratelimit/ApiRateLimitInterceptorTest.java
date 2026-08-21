package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.global.client.ClientContextProperties;
import com.umc.product.global.client.ClientOriginRegistry;
import com.umc.product.global.client.ClientRequestClassifier;
import com.umc.product.global.config.LoggingInterceptor;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.response.ApiErrorResponseWriter;
import com.umc.product.global.security.MemberPrincipal;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ApiRateLimitInterceptorTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 POST /graphql 요청은 인증 사용자 기본 한도를 적용한다")
    void graphql_authenticated_request_uses_authenticated_default_policy() throws Exception {
        GraphQlController controller = new GraphQlController();
        MockMvc mockMvc = mockMvc(controller, ApiRateLimitProperties.defaults());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(1L), null, List.of())
        );

        mockMvc.perform(post("/graphql"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "20"));

        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    @DisplayName("익명 POST /graphql 요청은 익명 기본 한도 초과 시 429를 반환한다")
    void graphql_anonymous_request_returns_429_after_default_limit() throws Exception {
        GraphQlController controller = new GraphQlController();
        MockMvc mockMvc = mockMvc(controller, ApiRateLimitProperties.defaults());

        for (int request = 0; request < 5; request++) {
            mockMvc.perform(post("/graphql"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "5"));
        }
        mockMvc.perform(post("/graphql"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(header().string("X-RateLimit-Limit", "5"))
            .andExpect(jsonPath("$.code").value(CommonErrorCode.TOO_MANY_REQUESTS.getCode()));

        assertThat(controller.invocations()).isEqualTo(5);
    }

    @Test
    @DisplayName("같은 IP의 게스트들은 참가자별 한도를 사용하고 한 게스트의 초과가 다른 게스트에 영향 주지 않는다")
    void demoday_guests_on_same_ip_use_independent_buckets() throws Exception {
        DemodayParticipationController controller = new DemodayParticipationController();
        MockMvc mockMvc = mockMvc(controller, guestLimitedProperties());

        authenticateGuest(101L);
        mockMvc.perform(get("/api/v1/demoday/polls/1/participations/me")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.10");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "1"));

        mockMvc.perform(get("/api/v1/demoday/polls/1/participations/me")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.10");
                    return request;
                }))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(CommonErrorCode.TOO_MANY_REQUESTS.getCode()));

        authenticateGuest(202L);
        mockMvc.perform(get("/api/v1/demoday/polls/1/participations/me")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.10");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "1"));

        assertThat(controller.invocations()).isEqualTo(2);
    }

    @Test
    @DisplayName("path variable 값이 달라도 같은 route pattern bucket을 공유해 한도 초과 시 429를 반환한다")
    void blocks_by_route_pattern_bucket() throws Exception {
        ProductController controller = new ProductController();
        MockMvc mockMvc = mockMvc(controller, limitedProperties(true));

        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/2"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(header().string("X-RateLimit-Limit", "1"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(CommonErrorCode.TOO_MANY_REQUESTS.getCode()));

        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    @DisplayName("rate limiter가 비활성화되면 같은 bucket 요청도 모두 통과한다")
    void disabled_limiter_allows_all_requests() throws Exception {
        ProductController controller = new ProductController();
        MockMvc mockMvc = mockMvc(controller, limitedProperties(false));

        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/2"))
            .andExpect(status().isOk());

        assertThat(controller.invocations()).isEqualTo(2);
    }

    @Test
    @DisplayName("OPTIONS preflight 요청은 토큰을 소비하지 않는다")
    void options_request_is_excluded() throws Exception {
        ProductController controller = new ProductController();
        MockMvc mockMvc = mockMvc(controller, limitedProperties(true));

        mockMvc.perform(options("/api/v1/products/1"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk());

        assertThat(controller.invocations()).isEqualTo(1);
    }

    private static MockMvc mockMvc(Object controller, ApiRateLimitProperties properties) {
        ApiRateLimitMetrics metrics = new ApiRateLimitMetrics(new SimpleMeterRegistry());
        ApiRateLimitInterceptor rateLimitInterceptor = new ApiRateLimitInterceptor(
            new RateLimitClientKeyResolver(),
            new RateLimitRouteResolver(),
            new RateLimitPolicyResolver(properties),
            new RateLimitBucketRegistry(properties),
            new ApiErrorResponseWriter(new ObjectMapper()),
            metrics
        );

        return MockMvcBuilders
            .standaloneSetup(controller)
            .addInterceptors(loggingInterceptor(), rateLimitInterceptor)
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    }

    private static LoggingInterceptor loggingInterceptor() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClientContextProperties properties = new ClientContextProperties(List.of());
        return new LoggingInterceptor(
            new ClientRequestClassifier(new ClientOriginRegistry(properties)),
            new OperationalMetrics(registry)
        );
    }

    private static ApiRateLimitProperties limitedProperties(boolean enabled) {
        ApiRateLimitProperties.Limit oneRequest = new ApiRateLimitProperties.Limit(1, 1);
        return new ApiRateLimitProperties(
            enabled,
            List.of("/api/**"),
            ApiRateLimitProperties.defaultExcludedPaths(),
            oneRequest,
            oneRequest,
            List.of(),
            new ApiRateLimitProperties.Cache(10_000, Duration.ofMinutes(10))
        );
    }

    private static ApiRateLimitProperties guestLimitedProperties() {
        ApiRateLimitProperties.Limit oneRequest = new ApiRateLimitProperties.Limit(1, 1);
        ApiRateLimitProperties.Limit anonymousLimit = new ApiRateLimitProperties.Limit(5, 5);
        return new ApiRateLimitProperties(
            true,
            List.of("/api/**"),
            ApiRateLimitProperties.defaultExcludedPaths(),
            oneRequest,
            anonymousLimit,
            List.of(),
            new ApiRateLimitProperties.Cache(10_000, Duration.ofMinutes(10))
        );
    }

    private static void authenticateGuest(Long entryCodeId) {
        DemodayParticipationPrincipal principal = new DemodayParticipationPrincipal(entryCodeId);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @RestController
    private static class ProductController {

        private final AtomicInteger invocations = new AtomicInteger();

        @GetMapping("/api/v1/products/{productId}")
        String product(@PathVariable Long productId) {
            invocations.incrementAndGet();
            return "product-" + productId;
        }

        @RequestMapping(method = RequestMethod.OPTIONS, path = "/api/v1/products/{productId}")
        void options() {
        }

        int invocations() {
            return invocations.get();
        }
    }

    @RestController
    private static class GraphQlController {

        private final AtomicInteger invocations = new AtomicInteger();

        @RequestMapping(method = RequestMethod.POST, path = "/graphql")
        String graphql() {
            invocations.incrementAndGet();
            return "graphql";
        }

        int invocations() {
            return invocations.get();
        }
    }

    @RestController
    private static class DemodayParticipationController {

        private final AtomicInteger invocations = new AtomicInteger();

        @GetMapping("/api/v1/demoday/polls/{pollId}/participations/me")
        String participation(@PathVariable Long pollId) {
            invocations.incrementAndGet();
            return "participation-" + pollId;
        }

        int invocations() {
            return invocations.get();
        }
    }
}
