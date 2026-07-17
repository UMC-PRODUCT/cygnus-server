package com.umc.product.registry.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record RegistrySourceReconciliation(
    String sourceName,
    Map<RegistryDriftType, Long> counts,
    List<String> details
) {

    public RegistrySourceReconciliation {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("source name은 필수입니다.");
        }
        EnumMap<RegistryDriftType, Long> normalized = new EnumMap<>(RegistryDriftType.class);
        if (counts != null) {
            counts.forEach((type, count) -> {
                if (type == null || count == null || count < 0) {
                    throw new IllegalArgumentException("drift count는 유효해야 합니다.");
                }
                if (count > 0) {
                    normalized.put(type, count);
                }
            });
        }
        counts = Collections.unmodifiableMap(normalized);
        details = details == null ? List.of() : details.stream().sorted().toList();
    }

    public long totalDriftCount() {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }
}
