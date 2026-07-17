package com.umc.product.storage.adapter.out.backfill;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

@Component
@Profile("registry-backfill")
public class StorageFileUsageRolloutAdapter implements RegistryRolloutPort {

    public static final String REGISTRY_NAME = "file-usage";

    private final JdbcTemplate jdbcTemplate;
    private final StorageFileUsageReconciler reconciler;

    public StorageFileUsageRolloutAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.reconciler = new StorageFileUsageReconciler(jdbcTemplate);
    }

    @Override
    public String registryName() {
        return REGISTRY_NAME;
    }

    @Override
    public List<String> sourceNames() {
        return StorageFileUsageSource.ALL.stream()
            .map(StorageFileUsageSource::sourceName)
            .toList();
    }

    @Override
    public void preflight() {
    }

    @Override
    public void startBackfill(Instant cutoverAt) {
        jdbcTemplate.update("""
            UPDATE file_metadata
            SET confirmed_at = COALESCE(updated_at, created_at)
            WHERE is_uploaded = TRUE AND confirmed_at IS NULL
            """);
    }

    @Override
    public RegistryBackfillBatch backfillBatch(
        String sourceName,
        long afterParentId,
        int batchSize
    ) {
        StorageFileUsageSource source = requireSource(sourceName);
        List<Long> parentIds = jdbcTemplate.queryForList(
            source.parentIdsSql(), Long.class, afterParentId, batchSize);
        if (parentIds.isEmpty()) {
            return new RegistryBackfillBatch(afterParentId, 0L, true);
        }
        long lastParentId = parentIds.getLast();
        List<StorageFileUsageReference> references = jdbcTemplate.query(
            source.batchReferencesSql(),
            (resultSet, rowNum) -> new StorageFileUsageReference(
                resultSet.getLong("parent_id"),
                resultSet.getString("resource_key"),
                resultSet.getString("file_id")
            ),
            afterParentId,
            lastParentId
        );
        references.forEach(reference -> insertReference(source, reference));
        return new RegistryBackfillBatch(
            lastParentId,
            parentIds.size(),
            parentIds.size() < batchSize
        );
    }

    @Override
    public void beforeValidation(Instant cutoverAt) {
        jdbcTemplate.update("""
            UPDATE file_metadata metadata
            SET unreferenced_at = CASE
                WHEN EXISTS (
                    SELECT 1 FROM file_usage usage WHERE usage.file_id = metadata.id
                ) THEN NULL
                ELSE CAST(? AS TIMESTAMP WITH TIME ZONE)
            END
            """, Timestamp.from(cutoverAt));
    }

    @Override
    public RegistryReconciliationResult reconcile(int detailLimit) {
        List<RegistrySourceReconciliation> summaries = new ArrayList<>();
        StorageFileUsageSource.ALL.forEach(source ->
            summaries.add(reconciler.reconcile(source, detailLimit)));
        summaries.add(reconciler.reconcileMetadataLifecycle(detailLimit));
        return new RegistryReconciliationResult(REGISTRY_NAME, summaries);
    }

    private void insertReference(
        StorageFileUsageSource source,
        StorageFileUsageReference reference
    ) {
        jdbcTemplate.update("""
            INSERT INTO file_usage_owner
                (usage_namespace, resource_key, slot, created_at, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (usage_namespace, resource_key, slot) DO NOTHING
            """, source.usageNamespace(), reference.resourceKey(), source.slot());
        jdbcTemplate.update("""
            INSERT INTO file_usage (owner_id, file_id, created_at)
            SELECT owner.id, metadata.id, CURRENT_TIMESTAMP
            FROM file_usage_owner owner
            JOIN file_metadata metadata ON metadata.id = ?
            WHERE owner.usage_namespace = ?
              AND owner.resource_key = ?
              AND owner.slot = ?
            ON CONFLICT (owner_id, file_id) DO NOTHING
            """,
            reference.fileId(),
            source.usageNamespace(),
            reference.resourceKey(),
            source.slot()
        );
        jdbcTemplate.update(
            "UPDATE file_metadata SET unreferenced_at = NULL WHERE id = ?",
            reference.fileId()
        );
    }

    private StorageFileUsageSource requireSource(String sourceName) {
        return StorageFileUsageSource.ALL.stream()
            .filter(source -> source.sourceName().equals(sourceName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("알 수 없는 storage source: " + sourceName));
    }
}
