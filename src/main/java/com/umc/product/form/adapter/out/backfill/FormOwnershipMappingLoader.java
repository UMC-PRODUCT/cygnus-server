package com.umc.product.form.adapter.out.backfill;

import java.util.ArrayList;
import java.util.List;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;

final class FormOwnershipMappingLoader {

    private static final int SCAN_BATCH_SIZE = 500;

    List<FormOwnershipCandidate> loadAll(List<FormOwnershipBackfillSource> sources) {
        List<FormOwnershipCandidate> candidates = new ArrayList<>();
        sources.forEach(source -> loadAll(source).stream()
            .map(mapping -> FormOwnershipCandidate.from(source, mapping))
            .forEach(candidates::add));
        return List.copyOf(candidates);
    }

    List<FormOwnershipBackfillMapping> loadAll(FormOwnershipBackfillSource source) {
        List<FormOwnershipBackfillMapping> mappings = new ArrayList<>();
        long afterParentId = 0L;
        boolean completed = false;
        while (!completed) {
            List<FormOwnershipBackfillMapping> batch = source.loadBatch(
                afterParentId,
                SCAN_BATCH_SIZE
            );
            validateBatch(source, batch, afterParentId);
            mappings.addAll(batch);
            completed = batch.size() < SCAN_BATCH_SIZE;
            if (!batch.isEmpty()) {
                afterParentId = batch.getLast().parentId();
            }
        }
        return List.copyOf(mappings);
    }

    private void validateBatch(
        FormOwnershipBackfillSource source,
        List<FormOwnershipBackfillMapping> batch,
        long afterParentId
    ) {
        if (batch == null || batch.size() > SCAN_BATCH_SIZE) {
            throw new IllegalStateException("유효하지 않은 Form backfill batch: " + source.sourceName());
        }
        long previousParentId = afterParentId;
        for (FormOwnershipBackfillMapping mapping : batch) {
            if (mapping == null || mapping.parentId() <= previousParentId) {
                throw new IllegalStateException(
                    "Form backfill parent keyset이 단조 증가하지 않습니다: " + source.sourceName()
                );
            }
            previousParentId = mapping.parentId();
        }
    }
}
