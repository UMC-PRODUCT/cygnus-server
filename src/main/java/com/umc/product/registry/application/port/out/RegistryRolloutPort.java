package com.umc.product.registry.application.port.out;

import java.time.Instant;
import java.util.List;

import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryReconciliationResult;

public interface RegistryRolloutPort {

    String registryName();

    List<String> sourceNames();

    void preflight();

    default void startBackfill(Instant cutoverAt) {
    }

    RegistryBackfillBatch backfillBatch(String sourceName, long afterParentId, int batchSize);

    default void beforeValidation(Instant cutoverAt) {
    }

    RegistryReconciliationResult reconcile(int detailLimit);
}
