package com.umc.product.global.ratelimit;

public record RateLimitClientKey(
    String value,
    boolean authenticated,
    String clientType
) {
}
