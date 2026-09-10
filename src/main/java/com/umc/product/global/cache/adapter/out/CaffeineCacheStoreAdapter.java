package com.umc.product.global.cache.adapter.out;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umc.product.global.cache.application.port.out.CacheStorePort;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

@Component
public class CaffeineCacheStoreAdapter implements CacheStorePort {

    private final CacheKeyFormatter keyFormatter;
    private final MeterRegistry meterRegistry;
    private final Map<CacheNamespace, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final Set<CacheNamespace> monitoredNamespaces = ConcurrentHashMap.newKeySet();

    @Autowired
    public CaffeineCacheStoreAdapter(
        CacheKeyFormatter keyFormatter,
        ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        this.keyFormatter = keyFormatter;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
        Object value = getCache(spec).getIfPresent(keyFormatter.format(spec.namespace(), key));
        if (value == null) {
            return new CacheLookup.Miss<>();
        }
        return new CacheLookup.Hit<>(spec.valueType().cast(value));
    }

    @Override
    public <T> void put(CacheSpec<T> spec, CacheKey key, T value) {
        getCache(spec).put(keyFormatter.format(spec.namespace(), key), value);
    }

    @Override
    public void evict(CacheNamespace namespace, CacheKey key) {
        Cache<String, Object> cache = caches.get(namespace);
        if (cache != null) {
            cache.invalidate(keyFormatter.format(namespace, key));
        }
    }

    Cache<String, Object> nativeCache(CacheSpec<?> spec) {
        return getCache(spec);
    }

    private Cache<String, Object> getCache(CacheSpec<?> spec) {
        return caches.computeIfAbsent(spec.namespace(), ignored -> {
            Cache<String, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(spec.ttl())
                .maximumSize(spec.maximumSize())
                .recordStats()
                .build();
            monitorIfNecessary(spec.namespace(), cache);
            return cache;
        });
    }

    private void monitorIfNecessary(CacheNamespace namespace, Cache<String, Object> cache) {
        if (meterRegistry == null || !monitoredNamespaces.add(namespace)) {
            return;
        }
        CaffeineCacheMetrics.monitor(meterRegistry, cache, namespace.metricName());
    }
}
