package com.umc.product.registry.domain;

public record RegistryBackfillBatch(
    long lastParentId,
    long scannedParents,
    boolean completed
) {

    public RegistryBackfillBatch {
        if (lastParentId < 0 || scannedParents < 0) {
            throw new IllegalArgumentException("backfill batch 수치는 음수일 수 없습니다.");
        }
    }
}
