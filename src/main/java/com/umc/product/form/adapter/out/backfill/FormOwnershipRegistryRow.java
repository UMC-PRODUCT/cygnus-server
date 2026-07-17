package com.umc.product.form.adapter.out.backfill;

record FormOwnershipRegistryRow(
    long formId,
    String namespace,
    String ownerResourceKey,
    String slot
) {

    String display() {
        return namespace + "/" + ownerResourceKey + "/" + slot + "=" + formId;
    }
}
