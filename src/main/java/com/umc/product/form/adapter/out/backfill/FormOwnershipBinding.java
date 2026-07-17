package com.umc.product.form.adapter.out.backfill;

record FormOwnershipBinding(
    long formId,
    String namespace,
    String ownerResourceKey,
    String slot
) {

    static FormOwnershipBinding from(FormOwnershipCandidate candidate) {
        return new FormOwnershipBinding(
            candidate.formId(),
            candidate.namespace(),
            candidate.ownerResourceKey(),
            candidate.slot()
        );
    }

    static FormOwnershipBinding from(FormOwnershipRegistryRow row) {
        return new FormOwnershipBinding(
            row.formId(),
            row.namespace(),
            row.ownerResourceKey(),
            row.slot()
        );
    }
}
