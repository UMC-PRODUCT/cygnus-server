package com.umc.product.global.cache.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.cache.application.port.out.CacheStorePort;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("CacheService")
class CacheServiceTest {

    @Test
    @DisplayName("get, put, evict 요청을 저장소 포트로 위임한다")
    void cache_store_port_위임() {
        FakeCacheStorePort storePort = new FakeCacheStorePort();
        CacheService cacheService = new CacheService(storePort);
        CacheSpec<String> spec = CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ofMinutes(5),
            100L
        );
        CacheKey key = CacheKey.from("google");

        cacheService.put(spec, key, "auth");
        CacheLookup<String> hit = cacheService.get(spec, key);
        cacheService.evict(CacheNamespace.GOOGLE_JWKS, key);
        CacheLookup<String> miss = cacheService.get(spec, key);

        assertThat(hit).isInstanceOf(CacheLookup.Hit.class);
        assertThat(((CacheLookup.Hit<String>) hit).value()).isEqualTo("auth");
        assertThat(miss).isInstanceOf(CacheLookup.Miss.class);
    }

    private static class FakeCacheStorePort implements CacheStorePort {

        private Object value;

        @Override
        public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
            if (value == null) {
                return new CacheLookup.Miss<>();
            }
            return new CacheLookup.Hit<>(spec.valueType().cast(value));
        }

        @Override
        public <T> void put(CacheSpec<T> spec, CacheKey key, T value) {
            this.value = value;
        }

        @Override
        public void evict(CacheNamespace namespace, CacheKey key) {
            this.value = null;
        }
    }
}
