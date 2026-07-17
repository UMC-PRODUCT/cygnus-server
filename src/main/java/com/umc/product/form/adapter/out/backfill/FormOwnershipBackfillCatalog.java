package com.umc.product.form.adapter.out.backfill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;

final class FormOwnershipBackfillCatalog {

    private final List<FormOwnershipBackfillSource> sources;
    private final Map<String, FormOwnershipBackfillSource> byName;

    FormOwnershipBackfillCatalog(List<FormOwnershipBackfillSource> sources) {
        this.sources = normalizedSources(sources);
        this.byName = indexSources(this.sources);
        validateCoordinates(this.sources);
    }

    List<FormOwnershipBackfillSource> sources() {
        return sources;
    }

    List<String> sourceNames() {
        List<String> sourceNames = new ArrayList<>(byName.keySet());
        sourceNames.add(FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME);
        return List.copyOf(sourceNames);
    }

    FormOwnershipBackfillSource require(String sourceName) {
        FormOwnershipBackfillSource source = byName.get(sourceName);
        if (source == null) {
            throw new IllegalArgumentException("알 수 없는 Form ownership source: " + sourceName);
        }
        return source;
    }

    void preflight(FormOwnershipMappingLoader mappingLoader) {
        sources.forEach(FormOwnershipBackfillSource::preflight);
        List<FormOwnershipCandidate> candidates = mappingLoader.loadAll(sources);
        Map<Long, Set<String>> coordinatesByForm = new HashMap<>();
        Map<String, Set<Long>> formsByCoordinate = new HashMap<>();
        candidates.forEach(candidate -> {
            coordinatesByForm.computeIfAbsent(candidate.formId(), ignored -> new HashSet<>())
                .add(candidate.coordinate());
            formsByCoordinate.computeIfAbsent(candidate.coordinate(), ignored -> new HashSet<>())
                .add(candidate.formId());
        });
        rejectFormWithMultipleOwners(coordinatesByForm);
        rejectOwnerWithMultipleForms(formsByCoordinate);
    }

    private static void rejectFormWithMultipleOwners(Map<Long, Set<String>> coordinatesByForm) {
        coordinatesByForm.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .sorted(Map.Entry.comparingByKey())
            .findFirst()
            .ifPresent(entry -> {
                throw new IllegalStateException(
                    "동일 Form이 복수 owner를 가집니다: " + entry.getKey()
                );
            });
    }

    private static void rejectOwnerWithMultipleForms(Map<String, Set<Long>> formsByCoordinate) {
        formsByCoordinate.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .sorted(Map.Entry.comparingByKey())
            .findFirst()
            .ifPresent(entry -> {
                throw new IllegalStateException(
                    "동일 owner tuple이 복수 Form을 가집니다: " + entry.getKey()
                );
            });
    }

    private static List<FormOwnershipBackfillSource> normalizedSources(
        List<FormOwnershipBackfillSource> sources
    ) {
        if (sources == null) {
            throw new IllegalArgumentException("Form ownership sources는 null일 수 없습니다.");
        }
        return sources.stream()
            .sorted((left, right) -> left.sourceName().compareTo(right.sourceName()))
            .toList();
    }

    private static Map<String, FormOwnershipBackfillSource> indexSources(
        List<FormOwnershipBackfillSource> sources
    ) {
        Map<String, FormOwnershipBackfillSource> indexed = new LinkedHashMap<>();
        for (FormOwnershipBackfillSource source : sources) {
            if (source == null || source.sourceName() == null || source.sourceName().isBlank()) {
                throw new IllegalArgumentException("Form ownership source name은 필수입니다.");
            }
            if (indexed.putIfAbsent(source.sourceName(), source) != null) {
                throw new IllegalArgumentException(
                    "중복 Form ownership source name: " + source.sourceName()
                );
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    private static void validateCoordinates(List<FormOwnershipBackfillSource> sources) {
        Set<String> coordinates = new HashSet<>();
        for (FormOwnershipBackfillSource source : sources) {
            String coordinate = source.namespace() + "/" + source.slot();
            if (!coordinates.add(coordinate)) {
                throw new IllegalArgumentException(
                    "중복 Form ownership source coordinate: " + coordinate
                );
            }
        }
    }
}
