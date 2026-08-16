package com.umc.product.global.ratelimit;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Component
public class RateLimitBucketRegistry {

    private final Cache<String, Bucket> buckets;

    public RateLimitBucketRegistry(ApiRateLimitProperties properties) {
        this.buckets = Caffeine.newBuilder()
            .maximumSize(properties.cache().maximumSize())
            .expireAfterAccess(properties.cache().expireAfterAccess())
            .build();
    }

    public Bucket get(String key, RateLimitPolicy policy) {
        return buckets.get(key, ignored -> createBucket(policy));
    }

    private Bucket createBucket(RateLimitPolicy policy) {
        return Bucket.builder()
            .addLimit(greedyLimit(policy.requestsPerSecond(), Duration.ofSeconds(1)))
            .addLimit(greedyLimit(policy.requestsPerMinute(), Duration.ofMinutes(1)))
            .build();
    }

    private Bandwidth greedyLimit(int capacity, Duration refillPeriod) {
        return Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, refillPeriod)
            .build();
    }
}
