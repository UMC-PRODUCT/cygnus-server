package com.umc.product.form.adapter.out.backfill;

record FormOwnershipCoordinate(String namespace, String ownerResourceKey, String slot) {

    static FormOwnershipCoordinate from(FormOwnershipCandidate candidate) {
        return new FormOwnershipCoordinate(
            candidate.namespace(),
            candidate.ownerResourceKey(),
            candidate.slot()
        );
    }

    static FormOwnershipCoordinate from(FormOwnershipRegistryRow row) {
        return new FormOwnershipCoordinate(row.namespace(), row.ownerResourceKey(), row.slot());
    }
}
