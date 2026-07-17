package com.umc.product.form.adapter.out.backfill;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;

record FormOwnershipCandidate(
    String sourceName,
    long parentId,
    long formId,
    String namespace,
    String ownerResourceKey,
    String slot
) {

    static FormOwnershipCandidate from(
        FormOwnershipBackfillSource source,
        FormOwnershipBackfillMapping mapping
    ) {
        return new FormOwnershipCandidate(
            source.sourceName(),
            mapping.parentId(),
            mapping.formId(),
            source.namespace(),
            mapping.ownerResourceKey(),
            source.slot()
        );
    }

    String coordinate() {
        return namespace + "/" + ownerResourceKey + "/" + slot;
    }

    String binding() {
        return coordinate() + "=" + formId;
    }
}
