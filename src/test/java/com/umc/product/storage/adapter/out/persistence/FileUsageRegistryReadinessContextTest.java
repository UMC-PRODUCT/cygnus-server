package com.umc.product.storage.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.support.IntegrationTestSupport;

class FileUsageRegistryReadinessContextTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway 이후 certification 전에는 Storage registry readiness가 DISABLED다")
    void certification_전에는_Storage_registry가_DISABLED다() {
        // given
        Map<String, FileUsageRegistryReadinessPort> readinessBeans = applicationContext
            .getBeansOfType(FileUsageRegistryReadinessPort.class);

        // when
        assertThat(readinessBeans).containsOnlyKeys("registryFileUsageReadinessAdapter");

        // then
        FileUsageRegistryReadinessPort readinessPort = readinessBeans.values().iterator().next();
        assertThat(readinessPort.getStatus()).isEqualTo(FileUsageRegistryStatus.DISABLED);
        assertThat(readinessPort.isReady()).isFalse();
    }

    @Test
    @DisplayName("exact reconciliation이 인증되면 Storage registry readiness가 READY다")
    void certification_후에는_Storage_registry가_READY다() {
        jdbcTemplate.update("""
            INSERT INTO file_usage_registry_cutover
                (singleton, status, verified_at, created_at, updated_at)
            VALUES (TRUE, 'READY', NOW(), NOW(), NOW())
            ON CONFLICT (singleton) DO UPDATE
            SET status = 'READY', verified_at = NOW(), updated_at = NOW()
            """);
        FileUsageRegistryReadinessPort readinessPort = applicationContext
            .getBeansOfType(FileUsageRegistryReadinessPort.class)
            .values()
            .iterator()
            .next();

        assertThat(readinessPort.getStatus()).isEqualTo(FileUsageRegistryStatus.READY);
        assertThat(readinessPort.isReady()).isTrue();
    }
}
