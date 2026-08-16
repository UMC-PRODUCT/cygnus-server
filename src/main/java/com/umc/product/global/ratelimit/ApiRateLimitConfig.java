package com.umc.product.global.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.global.response.ApiErrorResponseWriter;

@Configuration
@EnableConfigurationProperties(ApiRateLimitProperties.class)
public class ApiRateLimitConfig {

    @Bean
    public ApiRateLimitInterceptor apiRateLimitInterceptor(
        RateLimitClientKeyResolver clientKeyResolver,
        RateLimitRouteResolver routeResolver,
        RateLimitPolicyResolver policyResolver,
        RateLimitBucketRegistry bucketRegistry,
        ApiErrorResponseWriter errorResponseWriter,
        ApiRateLimitMetrics metrics
    ) {
        return new ApiRateLimitInterceptor(
            clientKeyResolver,
            routeResolver,
            policyResolver,
            bucketRegistry,
            errorResponseWriter,
            metrics
        );
    }
}
