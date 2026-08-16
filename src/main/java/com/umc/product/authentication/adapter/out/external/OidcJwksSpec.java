package com.umc.product.authentication.adapter.out.external;

import java.time.Duration;

import com.umc.product.global.cache.domain.CacheNamespace;

public record OidcJwksSpec(
    CacheNamespace namespace,
    String jwksUri,
    Duration ttl,
    long maxSize
) {
}
