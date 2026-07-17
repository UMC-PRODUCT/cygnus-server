package com.umc.product.form.adapter.out.backfill;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryReconciliationResult;

@Component
@Profile("registry-backfill")
public class FormOwnershipRolloutAdapter implements RegistryRolloutPort {

    public static final String REGISTRY_NAME = "form-ownership";
    public static final String STANDALONE_SOURCE_NAME = "form-standalone";
    public static final String STANDALONE_NAMESPACE = "form.standalone";
    public static final String DEFAULT_SLOT = "default";

    private final JdbcTemplate jdbcTemplate;
    private final FormOwnershipBackfillCatalog catalog;
    private final FormOwnershipMappingLoader mappingLoader;
    private final FormOwnershipReconciler reconciler;

    public FormOwnershipRolloutAdapter(
        JdbcTemplate jdbcTemplate,
        List<FormOwnershipBackfillSource> sources
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalog = new FormOwnershipBackfillCatalog(sources);
        this.mappingLoader = new FormOwnershipMappingLoader();
        this.reconciler = new FormOwnershipReconciler(
            jdbcTemplate,
            catalog.sources(),
            mappingLoader
        );
    }

    @Override
    public String registryName() {
        return REGISTRY_NAME;
    }

    @Override
    public List<String> sourceNames() {
        return catalog.sourceNames();
    }

    @Override
    public void preflight() {
        catalog.preflight(mappingLoader);
    }

    @Override
    public RegistryBackfillBatch backfillBatch(
        String sourceName,
        long afterParentId,
        int batchSize
    ) {
        validateBatchRequest(afterParentId, batchSize);
        if (STANDALONE_SOURCE_NAME.equals(sourceName)) {
            return backfillStandalone(afterParentId, batchSize);
        }
        FormOwnershipBackfillSource source = catalog.require(sourceName);
        List<FormOwnershipBackfillMapping> mappings = source.loadBatch(afterParentId, batchSize);
        validateMappings(source, mappings, afterParentId, batchSize);
        mappings.stream()
            .map(mapping -> FormOwnershipCandidate.from(source, mapping))
            .filter(this::isValid)
            .forEach(this::insertIfAbsent);
        long lastParentId = mappings.isEmpty()
            ? afterParentId
            : mappings.getLast().parentId();
        return new RegistryBackfillBatch(
            lastParentId,
            mappings.size(),
            mappings.size() < batchSize
        );
    }

    @Override
    public RegistryReconciliationResult reconcile(int detailLimit) {
        return reconciler.reconcile(detailLimit);
    }

    static FormOwnershipCandidate standaloneCandidate(long formId) {
        return new FormOwnershipCandidate(
            STANDALONE_SOURCE_NAME,
            formId,
            formId,
            STANDALONE_NAMESPACE,
            Long.toString(formId),
            DEFAULT_SLOT
        );
    }

    private RegistryBackfillBatch backfillStandalone(long afterParentId, int batchSize) {
        List<Long> formIds = jdbcTemplate.queryForList("""
            SELECT current_form.id
            FROM form current_form
            WHERE current_form.id > ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM form_ownership ownership
                  WHERE ownership.form_id = current_form.id
              )
            ORDER BY current_form.id
            LIMIT ?
            """, Long.class, afterParentId, batchSize);
        formIds.stream()
            .map(FormOwnershipRolloutAdapter::standaloneCandidate)
            .forEach(this::insertIfAbsent);
        long lastParentId = formIds.isEmpty() ? afterParentId : formIds.getLast();
        return new RegistryBackfillBatch(
            lastParentId,
            formIds.size(),
            formIds.size() < batchSize
        );
    }

    private void insertIfAbsent(FormOwnershipCandidate candidate) {
        jdbcTemplate.update("""
            INSERT INTO form_ownership
                (form_id, namespace, owner_resource_key, slot, created_at, updated_at)
            SELECT ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            WHERE EXISTS (SELECT 1 FROM form WHERE id = ?)
            ON CONFLICT DO NOTHING
            """,
            candidate.formId(),
            candidate.namespace(),
            candidate.ownerResourceKey(),
            candidate.slot(),
            candidate.formId()
        );
    }

    private void validateBatchRequest(long afterParentId, int batchSize) {
        if (afterParentId < 0 || batchSize <= 0) {
            throw new IllegalArgumentException(
                "parent id는 음수가 아니고 batch size는 양수여야 합니다."
            );
        }
    }

    private void validateMappings(
        FormOwnershipBackfillSource source,
        List<FormOwnershipBackfillMapping> mappings,
        long afterParentId,
        int batchSize
    ) {
        if (mappings == null || mappings.size() > batchSize) {
            throw new IllegalStateException("유효하지 않은 Form backfill batch: " + source.sourceName());
        }
        long previousParentId = afterParentId;
        for (FormOwnershipBackfillMapping mapping : mappings) {
            if (mapping == null || mapping.parentId() <= previousParentId) {
                throw new IllegalStateException(
                    "Form backfill parent keyset이 단조 증가하지 않습니다: " + source.sourceName()
                );
            }
            previousParentId = mapping.parentId();
        }
    }

    private boolean isValid(FormOwnershipCandidate candidate) {
        try {
            FormOwnerReference.of(
                candidate.formId(),
                candidate.namespace(),
                candidate.ownerResourceKey(),
                candidate.slot()
            );
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

}
