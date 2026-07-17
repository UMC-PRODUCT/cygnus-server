package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryAdvisoryLockAdapter;
import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryControlAdapter;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.storage.adapter.out.backfill.StorageFileUsageRolloutAdapter;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Registry rollout control PostgreSQL 통합")
class RegistryReconciliationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetControlRows() {
        jdbcTemplate.update("DELETE FROM registry_backfill_checkpoint");
        jdbcTemplate.update("DELETE FROM registry_cutover_state");
    }

    @Test
    void source별_checkpoint와_cutover_state를_독립적으로_저장한다() {
        // given
        jdbcTemplate.update("""
            INSERT INTO registry_backfill_checkpoint
                (registry_name, source_name, last_parent_id, processed_rows, completed, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, "file-usage", "member-profile-image", 41L, 12L, false);
        jdbcTemplate.update("""
            INSERT INTO registry_cutover_state (registry_name, status, verified_at, details)
            VALUES (?, ?, NULL, ?)
            """, "file-usage", "BACKFILLING", "batch in progress");

        // when
        Long checkpoint = jdbcTemplate.queryForObject("""
            SELECT last_parent_id
            FROM registry_backfill_checkpoint
            WHERE registry_name = ? AND source_name = ?
            """, Long.class, "file-usage", "member-profile-image");
        String status = jdbcTemplate.queryForObject("""
            SELECT status
            FROM registry_cutover_state
            WHERE registry_name = ?
            """, String.class, "file-usage");

        // then
        assertThat(checkpoint).isEqualTo(41L);
        assertThat(status).isEqualTo("BACKFILLING");
    }

    @Test
    void 정의되지_않은_cutover_status는_PostgreSQL이_거부한다() {
        // given
        String invalidStatus = "RUNNING";

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update("""
            INSERT INTO registry_cutover_state (registry_name, status, verified_at, details)
            VALUES (?, ?, NULL, ?)
            """, "file-usage", invalidStatus, "invalid"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 동일_registry의_advisory_lock은_동시에_하나만_획득한다() {
        // given
        PostgresRegistryAdvisoryLockAdapter lockAdapter =
            new PostgresRegistryAdvisoryLockAdapter(dataSource);

        // when & then
        try (var first = lockAdapter.acquire(StorageFileUsageRolloutAdapter.REGISTRY_NAME)) {
            assertThatThrownBy(() ->
                lockAdapter.acquire(StorageFileUsageRolloutAdapter.REGISTRY_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("advisory lock");
        }
        try (var reacquired = lockAdapter.acquire(StorageFileUsageRolloutAdapter.REGISTRY_NAME)) {
            assertThat(reacquired).isNotNull();
        }
    }

    @Test
    void reconcile은_control과_registry를_쓰지_않고_deterministic_summary를_반환한다() {
        // given
        StorageFileUsageRolloutAdapter rollout = new StorageFileUsageRolloutAdapter(jdbcTemplate);
        RegistryBackfillCoordinator coordinator = new RegistryBackfillCoordinator(
            List.of(rollout),
            new PostgresRegistryControlAdapter(jdbcTemplate),
            new PostgresRegistryAdvisoryLockAdapter(dataSource),
            Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC),
            500,
            20
        );
        long controlRowsBefore = countRows("registry_backfill_checkpoint")
            + countRows("registry_cutover_state");
        long registryRowsBefore = countRows("file_usage_owner") + countRows("file_usage");

        // when
        List<RegistryReconciliationResult> first = coordinator.reconcileAll();
        List<RegistryReconciliationResult> second = coordinator.reconcileAll();

        // then
        assertThat(first).isEqualTo(second);
        assertThat(countRows("registry_backfill_checkpoint")
            + countRows("registry_cutover_state")).isEqualTo(controlRowsBefore);
        assertThat(countRows("file_usage_owner") + countRows("file_usage"))
            .isEqualTo(registryRowsBefore);
    }

    private long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
