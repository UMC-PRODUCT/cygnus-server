package com.umc.product.registry.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryStatus;
import com.umc.product.registry.support.ControllableStoragePort.CleanupClaimObservation;

public class RegistryEndToEndDatabaseProbe {

    private final JdbcTemplate jdbcTemplate;

    public RegistryEndToEndDatabaseProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FileState fileState(String fileId) {
        return jdbcTemplate.queryForObject("""
            SELECT is_uploaded, confirmed_at, unreferenced_at, cleanup_claim_token,
                   cleanup_claimed_at, cleanup_attempts, cleanup_next_attempt_at,
                   cleanup_failed_at
            FROM file_metadata
            WHERE id = ?
            """, (resultSet, rowNumber) -> new FileState(
                resultSet.getBoolean("is_uploaded"),
                instant(resultSet.getTimestamp("confirmed_at")),
                instant(resultSet.getTimestamp("unreferenced_at")),
                resultSet.getObject("cleanup_claim_token", UUID.class),
                instant(resultSet.getTimestamp("cleanup_claimed_at")),
                resultSet.getInt("cleanup_attempts"),
                instant(resultSet.getTimestamp("cleanup_next_attempt_at")),
                instant(resultSet.getTimestamp("cleanup_failed_at"))
            ), fileId);
    }

    public List<String> usageRows(String fileId) {
        return jdbcTemplate.queryForList("""
            SELECT owner.usage_namespace || '/' || owner.resource_key || '/' || owner.slot
            FROM file_usage usage
            JOIN file_usage_owner owner ON owner.id = usage.owner_id
            WHERE usage.file_id = ?
            ORDER BY owner.usage_namespace, owner.resource_key, owner.slot
            """, String.class, fileId);
    }

    public long metadataCount(String fileId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_metadata WHERE id = ?", Long.class, fileId);
    }

    public long formSectionCount(Long formId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM form_section WHERE form_id = ?", Long.class, formId);
    }

    public CleanupClaimObservation claimObservation(String fileId) {
        FileState state = fileState(fileId);
        return new CleanupClaimObservation(
            state.cleanupClaimToken(), state.cleanupClaimedAt(), state.cleanupAttempts());
    }

    public void setStorageStatus(RegistryStatus status, Instant now) {
        jdbcTemplate.update("""
            INSERT INTO registry_cutover_state
                (registry_name, status, verified_at, details, updated_at)
            VALUES (?, ?, ?, 'registry E2E', CURRENT_TIMESTAMP)
            ON CONFLICT (registry_name) DO UPDATE
            SET status = EXCLUDED.status,
                verified_at = EXCLUDED.verified_at,
                details = EXCLUDED.details,
                updated_at = EXCLUDED.updated_at
            """,
            RegistryName.STORAGE_USAGE.canonicalName(),
            status.name(),
            status == RegistryStatus.READY ? Timestamp.from(now) : null
        );
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record FileState(
        boolean uploaded,
        Instant confirmedAt,
        Instant unreferencedAt,
        UUID cleanupClaimToken,
        Instant cleanupClaimedAt,
        int cleanupAttempts,
        Instant cleanupNextAttemptAt,
        Instant cleanupFailedAt
    ) {
    }
}
