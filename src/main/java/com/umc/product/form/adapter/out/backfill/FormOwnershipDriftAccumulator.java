package com.umc.product.form.adapter.out.backfill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

final class FormOwnershipDriftAccumulator {

    private final String sourceName;
    private final int detailLimit;
    private final Map<RegistryDriftType, TreeSet<String>> details =
        new EnumMap<>(RegistryDriftType.class);

    FormOwnershipDriftAccumulator(String sourceName, int detailLimit) {
        this.sourceName = sourceName;
        this.detailLimit = detailLimit;
    }

    void add(RegistryDriftType type, String detail) {
        details.computeIfAbsent(type, ignored -> new TreeSet<>())
            .add(type + ":" + detail);
    }

    RegistrySourceReconciliation result() {
        Map<RegistryDriftType, Long> counts = new EnumMap<>(RegistryDriftType.class);
        details.forEach((type, typedDetails) -> counts.put(type, (long) typedDetails.size()));
        List<String> boundedDetails = new ArrayList<>();
        for (RegistryDriftType type : RegistryDriftType.values()) {
            for (String detail : details.getOrDefault(type, new TreeSet<>())) {
                if (boundedDetails.size() == detailLimit) {
                    return new RegistrySourceReconciliation(sourceName, counts, boundedDetails);
                }
                boundedDetails.add(detail);
            }
        }
        return new RegistrySourceReconciliation(sourceName, counts, boundedDetails);
    }
}
