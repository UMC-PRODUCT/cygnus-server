package com.umc.product.global.cache.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

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

    @Test
    @DisplayName("생성되지 않은 namespace를 제거해도 예외가 발생하지 않는다")
    void cache_evict_생성되지_않은_namespace() {
        CaffeineCacheStoreAdapter adapter = newAdapter();

        adapter.evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member"));

        assertThat(adapter.nativeCache(spec()).estimatedSize()).isZero();
    }

    @Test
    @DisplayName("동일 namespace는 하나의 native cache를 재사용하고 metric을 한 번 등록한다")
    void native_cache_재사용과_metric_등록() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("meterRegistry", registry);
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider =
            beanFactory.getBeanProvider(io.micrometer.core.instrument.MeterRegistry.class);
        CaffeineCacheStoreAdapter adapter = new CaffeineCacheStoreAdapter(
            new CacheKeyFormatter("test"),
            provider
        );

        Object first = adapter.nativeCache(spec());
        Object second = adapter.nativeCache(spec());

        assertThat(second).isSameAs(first);
        assertThat(registry.getMeters()).isNotEmpty();
    }

    @Test
    @DisplayName("저장된 값의 타입이 spec과 다르면 조회를 거부한다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void cache_value_type_불일치() {
        CaffeineCacheStoreAdapter adapter = newAdapter();
        CacheSpec rawSpec = spec();
        adapter.put(rawSpec, CacheKey.from("google"), 1L);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> adapter.get(spec(), CacheKey.from("google"))
        ).isInstanceOf(ClassCastException.class);
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
