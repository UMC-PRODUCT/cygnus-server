package com.umc.product.registry.domain;

import java.util.Objects;

public record RegistryBackfillCheckpoint(
    String registryName,
    String sourceName,
    long lastParentId,
    long processedRows,
    boolean completed
) {

    public RegistryBackfillCheckpoint {
        requireText(registryName, "registry name");
        requireText(sourceName, "source name");
        if (lastParentId < 0 || processedRows < 0) {
            throw new IllegalArgumentException("checkpoint 수치는 음수일 수 없습니다.");
        }
    }

    public static RegistryBackfillCheckpoint initial(String registryName, String sourceName) {
        return new RegistryBackfillCheckpoint(registryName, sourceName, 0L, 0L, false);
    }

    public RegistryBackfillCheckpoint advance(RegistryBackfillBatch batch) {
        Objects.requireNonNull(batch, "backfill batch는 필수입니다.");
        if (batch.lastParentId() < lastParentId) {
            throw new IllegalStateException("checkpoint parent ID는 감소할 수 없습니다.");
        }
        if (!batch.completed() && batch.lastParentId() == lastParentId) {
            throw new IllegalStateException("미완료 batch는 checkpoint를 전진시켜야 합니다.");
        }
        return new RegistryBackfillCheckpoint(
            registryName,
            sourceName,
            batch.lastParentId(),
            Math.addExact(processedRows, batch.scannedParents()),
            batch.completed()
        );
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "은 필수입니다.");
        }
    }
}
