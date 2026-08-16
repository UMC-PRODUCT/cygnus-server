package com.umc.product.authorization.application.service;

import com.umc.product.global.cache.domain.CacheKey;

public final class AuthoritySnapshotCacheKeys {

    private AuthoritySnapshotCacheKeys() {
    }

    public static CacheKey member(Long memberId) {
        return CacheKey.from("member:" + memberId);
    }
}
