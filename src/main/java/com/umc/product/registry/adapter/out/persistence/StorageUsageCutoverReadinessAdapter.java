package com.umc.product.registry.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.out.StorageUsageCutoverReadinessPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StorageUsageCutoverReadinessAdapter implements StorageUsageCutoverReadinessPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isReady() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT EXISTS (
                SELECT 1
                FROM file_usage_registry_cutover
                WHERE singleton = TRUE
                  AND status = 'READY'
                  AND verified_at IS NOT NULL
            )
            """, Boolean.class));
    }
}
