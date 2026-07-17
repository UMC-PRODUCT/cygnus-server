package com.umc.product.storage.adapter.out.backfill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

final class StorageReconciliationAccumulator {

    private final String sourceName;
    private final int detailLimit;
    private final Map<RegistryDriftType, Long> counts = new EnumMap<>(RegistryDriftType.class);
    private final Map<RegistryDriftType, TreeSet<String>> details = new EnumMap<>(RegistryDriftType.class);

    StorageReconciliationAccumulator(String sourceName, int detailLimit) {
        this.sourceName = sourceName;
        this.detailLimit = detailLimit;
    }

    void add(RegistryDriftType type, String detail) {
        counts.merge(type, 1L, Long::sum);
        details.computeIfAbsent(type, ignored -> new TreeSet<>()).add(type + ":" + detail);
    }

    RegistrySourceReconciliation result() {
        List<String> bounded = new ArrayList<>();
        for (RegistryDriftType type : RegistryDriftType.values()) {
            TreeSet<String> typedDetails = details.getOrDefault(type, new TreeSet<>());
            for (String detail : typedDetails) {
                if (bounded.size() == detailLimit) {
                    return new RegistrySourceReconciliation(sourceName, counts, bounded);
                }
                bounded.add(detail);
            }
        }
        return new RegistrySourceReconciliation(sourceName, counts, bounded);
    }
}
