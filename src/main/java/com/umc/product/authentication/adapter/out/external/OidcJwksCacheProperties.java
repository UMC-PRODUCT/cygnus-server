package com.umc.product.authentication.adapter.out.external;

import java.time.Duration;

public record OidcJwksCacheProperties(
    Duration ttl,
    long maxSize
) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final long DEFAULT_MAX_SIZE = 1L;

    public OidcJwksCacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = DEFAULT_TTL;
        }
        if (maxSize <= 0) {
            maxSize = DEFAULT_MAX_SIZE;
        }
    }

    public static OidcJwksCacheProperties defaults() {
        return new OidcJwksCacheProperties(DEFAULT_TTL, DEFAULT_MAX_SIZE);
    }
}
