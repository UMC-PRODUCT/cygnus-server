package com.umc.product.form.application.port.out.dto;

public record FormOwnershipBackfillMapping(
    long parentId,
    long formId,
    String ownerResourceKey
) {

    public FormOwnershipBackfillMapping {
        if (parentId <= 0) {
            throw new IllegalArgumentException("backfill parent id는 양수여야 합니다.");
        }
    }
}
