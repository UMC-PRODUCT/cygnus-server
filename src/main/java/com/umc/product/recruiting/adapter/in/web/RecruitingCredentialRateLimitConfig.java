package com.umc.product.recruiting.adapter.in.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;
import com.umc.product.global.response.ApiErrorResponseWriter;

@Configuration
public class RecruitingCredentialRateLimitConfig {

    @Bean
    public RecruitingCredentialRestRateLimitInterceptor recruitingCredentialRestRateLimitInterceptor(
        RateLimitBucketRegistry bucketRegistry,
        ApiErrorResponseWriter errorResponseWriter,
        ApiRateLimitMetrics metrics
    ) {
        return new RecruitingCredentialRestRateLimitInterceptor(bucketRegistry, errorResponseWriter, metrics);
    }
}
