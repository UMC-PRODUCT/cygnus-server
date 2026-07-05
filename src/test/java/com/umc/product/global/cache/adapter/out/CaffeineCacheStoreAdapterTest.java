package com.umc.product.global.cache.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("CaffeineCacheStoreAdapter")
class CaffeineCacheStoreAdapterTest {

    @Test
    @DisplayName("저장되지 않은 key는 Miss를 반환한다")
    void cache_miss() {
        CaffeineCacheStoreAdapter adapter = newAdapter();
        CacheSpec<String> spec = spec();

        CacheLookup<String> result = adapter.get(spec, CacheKey.from("missing"));

        assertThat(result).isInstanceOf(CacheLookup.Miss.class);
    }

    @Test
    @DisplayName("put한 값은 Hit로 조회된다")
    void cache_hit() {
        CaffeineCacheStoreAdapter adapter = newAdapter();
        CacheSpec<String> spec = spec();

        adapter.put(spec, CacheKey.from("google"), "auth");
        CacheLookup<String> result = adapter.get(spec, CacheKey.from("google"));

        assertThat(result).isInstanceOf(CacheLookup.Hit.class);
        assertThat(((CacheLookup.Hit<String>) result).value()).isEqualTo("auth");
    }

    @Test
    @DisplayName("evict하면 다음 조회는 Miss가 된다")
    void cache_evict() {
        CaffeineCacheStoreAdapter adapter = newAdapter();
        CacheSpec<String> spec = spec();
        CacheKey key = CacheKey.from("google");

        adapter.put(spec, key, "auth");
        adapter.evict(CacheNamespace.GOOGLE_JWKS, key);

        assertThat(adapter.get(spec, key)).isInstanceOf(CacheLookup.Miss.class);
    }

    private CaffeineCacheStoreAdapter newAdapter() {
        return new CaffeineCacheStoreAdapter(new CacheKeyFormatter("test"));
    }

    private CacheSpec<String> spec() {
        return CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ofMinutes(5),
            100L
        );
    }
}
