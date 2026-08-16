package com.umc.product.global.ratelimit;

public record RateLimitPolicy(
    String name,
    int requestsPerSecond,
    int requestsPerMinute
) {
}
