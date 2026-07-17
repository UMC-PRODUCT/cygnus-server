package com.umc.product.registry.domain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record RegistryReconciliationResult(
    String registryName,
    List<RegistrySourceReconciliation> sources
) {

    public RegistryReconciliationResult {
        if (registryName == null || registryName.isBlank()) {
            throw new IllegalArgumentException("registry name은 필수입니다.");
        }
        sources = sources == null
            ? List.of()
            : sources.stream()
                .sorted(Comparator.comparing(RegistrySourceReconciliation::sourceName))
                .toList();
    }

    public long totalDriftCount() {
        return sources.stream().mapToLong(RegistrySourceReconciliation::totalDriftCount).sum();
    }

    public boolean isClean() {
        return totalDriftCount() == 0L;
    }

    public String summary() {
        String sourceSummary = sources.stream()
            .map(source -> source.sourceName() + source.counts())
            .collect(Collectors.joining(","));
        return "registry=%s,totalDrift=%d,sources=[%s]".formatted(
            registryName,
            totalDriftCount(),
            sourceSummary
        );
    }
}
