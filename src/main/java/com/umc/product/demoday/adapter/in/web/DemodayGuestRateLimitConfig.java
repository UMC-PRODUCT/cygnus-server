package com.umc.product.demoday.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;
import com.umc.product.global.ratelimit.RateLimitPolicy;
import com.umc.product.global.response.ApiErrorResponseWriter;

@Configuration
public class DemodayGuestRateLimitConfig {

    @Bean
    public DemodayGuestRateLimitInterceptor demodayGuestRateLimitInterceptor(
        RateLimitBucketRegistry bucketRegistry,
        ApiErrorResponseWriter errorResponseWriter,
        ApiRateLimitMetrics metrics,
        @Value("${demoday.guest-rate-limit.requests-per-second}") int requestsPerSecond,
        @Value("${demoday.guest-rate-limit.requests-per-minute}") int requestsPerMinute
    ) {
        RateLimitPolicy policy = new RateLimitPolicy("demoday-guest-participation", requestsPerSecond, requestsPerMinute);
        return new DemodayGuestRateLimitInterceptor(bucketRegistry, errorResponseWriter, metrics, policy);
    }
}
