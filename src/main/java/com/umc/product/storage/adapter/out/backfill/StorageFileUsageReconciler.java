package com.umc.product.storage.adapter.out.backfill;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

final class StorageFileUsageReconciler {

    private final JdbcTemplate jdbcTemplate;

    StorageFileUsageReconciler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    RegistrySourceReconciliation reconcile(StorageFileUsageSource source, int detailLimit) {
        List<StorageFileUsageReference> rawReferences = loadSourceReferences(source);
        StorageReconciliationAccumulator drift = new StorageReconciliationAccumulator(
            source.sourceName(), detailLimit);
        Map<RegistryKey, Long> frequencies = validFrequencies(rawReferences, source, drift);
        Set<RegistryKey> expected = new TreeSet<>(frequencies.keySet());
        Set<RegistryKey> actual = loadRegistryReferences(source);
        Map<String, MetadataState> metadata = loadMetadata();

        frequencies.forEach((key, count) -> {
            if (count > 1L) {
                drift.add(RegistryDriftType.DUPLICATE_REFERENCE, key.display(source));
            }
        });
        for (RegistryKey key : expected) {
            MetadataState state = metadata.get(key.fileId());
            if (state == null) {
                drift.add(RegistryDriftType.BROKEN_REFERENCE, key.display(source));
            } else if (!actual.contains(key)) {
                drift.add(RegistryDriftType.SOURCE_ONLY_MISSING, key.display(source));
            }
            if (state != null) {
                addLifecycleDrift(source, key, state, drift);
            }
        }
        actual.stream()
            .filter(key -> !expected.contains(key))
            .forEach(key -> drift.add(RegistryDriftType.REGISTRY_ONLY_STALE, key.display(source)));
        addOwnershipConflicts(source, expected, actual, drift);
        return drift.result();
    }

    RegistrySourceReconciliation reconcileMetadataLifecycle(int detailLimit) {
        StorageReconciliationAccumulator drift = new StorageReconciliationAccumulator(
            "file-metadata-lifecycle", detailLimit);
        Set<String> usedFileIds = new TreeSet<>(jdbcTemplate.queryForList(
            "SELECT DISTINCT file_id FROM file_usage ORDER BY file_id", String.class));
        Map<String, MetadataState> metadata = loadMetadata();
        metadata.forEach((fileId, state) -> {
            if (state.isUploaded() != (state.confirmedAt() != null)) {
                drift.add(RegistryDriftType.UPLOAD_CONFIRMATION_MISMATCH, fileId);
            }
            boolean lifecycleConsistent = usedFileIds.contains(fileId)
                ? state.unreferencedAt() == null
                : state.unreferencedAt() != null;
            if (!lifecycleConsistent) {
                drift.add(RegistryDriftType.LIFECYCLE_CONFLICT, fileId);
            }
        });
        return drift.result();
    }

    private List<StorageFileUsageReference> loadSourceReferences(StorageFileUsageSource source) {
        return jdbcTemplate.query(source.allReferencesSql(), (resultSet, rowNum) ->
            new StorageFileUsageReference(
                resultSet.getLong("parent_id"),
                resultSet.getString("resource_key"),
                resultSet.getString("file_id")
            ));
    }

    private Map<RegistryKey, Long> validFrequencies(
        List<StorageFileUsageReference> references,
        StorageFileUsageSource source,
        StorageReconciliationAccumulator drift
    ) {
        Map<RegistryKey, Long> frequencies = new TreeMap<>();
        for (StorageFileUsageReference reference : references) {
            if (!reference.isValid()) {
                drift.add(
                    RegistryDriftType.INVALID_REFERENCE,
                    source.usageNamespace() + "/" + reference.resourceKey() + "/" + source.slot()
                );
                continue;
            }
            frequencies.merge(
                new RegistryKey(reference.resourceKey(), reference.fileId()),
                1L,
                Long::sum
            );
        }
        return frequencies;
    }

    private Set<RegistryKey> loadRegistryReferences(StorageFileUsageSource source) {
        return new TreeSet<>(jdbcTemplate.query("""
            SELECT owner.resource_key, usage.file_id
            FROM file_usage_owner owner
            JOIN file_usage usage ON usage.owner_id = owner.id
            WHERE owner.usage_namespace = ? AND owner.slot = ?
            ORDER BY owner.resource_key, usage.file_id
            """, (resultSet, rowNum) -> new RegistryKey(
                resultSet.getString("resource_key"),
                resultSet.getString("file_id")
            ), source.usageNamespace(), source.slot()));
    }

    private Map<String, MetadataState> loadMetadata() {
        return jdbcTemplate.query("""
            SELECT id, is_uploaded, confirmed_at, unreferenced_at,
                   cleanup_claim_token, cleanup_failed_at
            FROM file_metadata
            ORDER BY id
            """, resultSet -> {
                Map<String, MetadataState> states = new LinkedHashMap<>();
                while (resultSet.next()) {
                    states.put(resultSet.getString("id"), new MetadataState(
                        resultSet.getBoolean("is_uploaded"),
                        instant(resultSet.getTimestamp("confirmed_at")),
                        instant(resultSet.getTimestamp("unreferenced_at")),
                        resultSet.getObject("cleanup_claim_token") != null,
                        resultSet.getTimestamp("cleanup_failed_at") != null
                    ));
                }
                return states;
            });
    }

    private void addLifecycleDrift(
        StorageFileUsageSource source,
        RegistryKey key,
        MetadataState state,
        StorageReconciliationAccumulator drift
    ) {
        if (!state.isUploaded() || state.confirmedAt() == null
            || state.unreferencedAt() != null || state.claimed() || state.failed()) {
            drift.add(RegistryDriftType.LIFECYCLE_CONFLICT, key.display(source));
        }
        if (state.isUploaded() != (state.confirmedAt() != null)) {
            drift.add(RegistryDriftType.UPLOAD_CONFIRMATION_MISMATCH, key.display(source));
        }
    }

    private void addOwnershipConflicts(
        StorageFileUsageSource source,
        Set<RegistryKey> expected,
        Set<RegistryKey> actual,
        StorageReconciliationAccumulator drift
    ) {
        Map<String, Set<String>> expectedByOwner = byOwner(expected);
        Map<String, Set<String>> actualByOwner = byOwner(actual);
        expectedByOwner.keySet().stream()
            .filter(actualByOwner::containsKey)
            .filter(owner -> !expectedByOwner.get(owner).equals(actualByOwner.get(owner)))
            .sorted()
            .forEach(owner -> drift.add(
                RegistryDriftType.OWNERSHIP_CONFLICT,
                source.usageNamespace() + "/" + owner + "/" + source.slot()
            ));
    }

    private Map<String, Set<String>> byOwner(Set<RegistryKey> keys) {
        return keys.stream().collect(Collectors.groupingBy(
            RegistryKey::resourceKey,
            TreeMap::new,
            Collectors.mapping(RegistryKey::fileId, Collectors.toCollection(TreeSet::new))
        ));
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record RegistryKey(String resourceKey, String fileId)
        implements Comparable<RegistryKey> {

        @Override
        public int compareTo(RegistryKey other) {
            int resourceOrder = resourceKey.compareTo(other.resourceKey);
            return resourceOrder != 0 ? resourceOrder : fileId.compareTo(other.fileId);
        }

        String display(StorageFileUsageSource source) {
            return source.usageNamespace() + "/" + resourceKey + "/" + source.slot() + "=" + fileId;
        }
    }

    private record MetadataState(
        boolean isUploaded,
        Instant confirmedAt,
        Instant unreferencedAt,
        boolean claimed,
        boolean failed
    ) {
    }
}
