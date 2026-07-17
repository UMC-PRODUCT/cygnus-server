package com.umc.product.registry.adapter.out.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.domain.RegistryBackfillCheckpoint;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryStatus;

import lombok.RequiredArgsConstructor;

@Component
@Profile("registry-backfill")
@RequiredArgsConstructor
public class PostgresRegistryControlAdapter implements RegistryControlPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RegistryCutoverState loadState(String registryName) {
        List<RegistryCutoverState> states = jdbcTemplate.query("""
            SELECT registry_name, status, verified_at, details
            FROM registry_cutover_state
            WHERE registry_name = ?
            """, (resultSet, rowNum) -> {
                Timestamp verifiedAt = resultSet.getTimestamp("verified_at");
                return new RegistryCutoverState(
                    resultSet.getString("registry_name"),
                    RegistryStatus.valueOf(resultSet.getString("status")),
                    verifiedAt == null ? null : verifiedAt.toInstant(),
                    resultSet.getString("details")
                );
            }, registryName);
        return states.stream().findFirst().orElseGet(() -> RegistryCutoverState.disabled(registryName));
    }

    @Override
    public boolean transition(
        String registryName,
        RegistryStatus expected,
        RegistryStatus target,
        Instant verifiedAt,
        String details
    ) {
        int updated = jdbcTemplate.update("""
            UPDATE registry_cutover_state
            SET status = ?, verified_at = ?, details = ?, updated_at = CURRENT_TIMESTAMP
            WHERE registry_name = ? AND status = ?
            """, target.name(), timestamp(verifiedAt), normalizedDetails(details),
            registryName, expected.name());
        if (updated == 1) {
            return true;
        }
        if (expected != RegistryStatus.DISABLED) {
            return false;
        }
        return jdbcTemplate.update("""
            INSERT INTO registry_cutover_state
                (registry_name, status, verified_at, details, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (registry_name) DO NOTHING
            """, registryName, target.name(), timestamp(verifiedAt), normalizedDetails(details)) == 1;
    }

    @Override
    public RegistryBackfillCheckpoint loadCheckpoint(String registryName, String sourceName) {
        List<RegistryBackfillCheckpoint> checkpoints = jdbcTemplate.query("""
            SELECT registry_name, source_name, last_parent_id, processed_rows, completed
            FROM registry_backfill_checkpoint
            WHERE registry_name = ? AND source_name = ?
            """, (resultSet, rowNum) -> new RegistryBackfillCheckpoint(
                resultSet.getString("registry_name"),
                resultSet.getString("source_name"),
                resultSet.getLong("last_parent_id"),
                resultSet.getLong("processed_rows"),
                resultSet.getBoolean("completed")
            ), registryName, sourceName);
        return checkpoints.stream().findFirst()
            .orElseGet(() -> RegistryBackfillCheckpoint.initial(registryName, sourceName));
    }

    @Override
    public void saveCheckpoint(RegistryBackfillCheckpoint checkpoint) {
        int changed = jdbcTemplate.update("""
            INSERT INTO registry_backfill_checkpoint
                (registry_name, source_name, last_parent_id, processed_rows, completed, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (registry_name, source_name) DO UPDATE
            SET last_parent_id = EXCLUDED.last_parent_id,
                processed_rows = EXCLUDED.processed_rows,
                completed = EXCLUDED.completed,
                updated_at = CURRENT_TIMESTAMP
            WHERE registry_backfill_checkpoint.last_parent_id <= EXCLUDED.last_parent_id
              AND registry_backfill_checkpoint.processed_rows <= EXCLUDED.processed_rows
            """,
            checkpoint.registryName(),
            checkpoint.sourceName(),
            checkpoint.lastParentId(),
            checkpoint.processedRows(),
            checkpoint.completed()
        );
        if (changed != 1) {
            throw new IllegalStateException("checkpoint의 단조 증가 조건을 위반했습니다.");
        }
    }

    @Override
    public void resetCheckpoints(String registryName, List<String> sourceNames) {
        if (sourceNames == null || sourceNames.stream().anyMatch(name -> name == null || name.isBlank())) {
            throw new IllegalArgumentException("reset source names는 유효해야 합니다.");
        }
        jdbcTemplate.update(
            "DELETE FROM registry_backfill_checkpoint WHERE registry_name = ?",
            registryName
        );
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static String normalizedDetails(String details) {
        return details == null ? "" : details;
    }
}
