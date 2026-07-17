package com.umc.product.form.adapter.out.backfill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.registry.domain.RegistryDriftType;

final class FormOwnershipExpectedIndex {

    private final Map<FormOwnershipBinding, FormOwnershipCandidate> byBinding;
    private final Map<Long, Set<FormOwnershipCandidate>> byForm;

    private FormOwnershipExpectedIndex(
        Map<FormOwnershipBinding, FormOwnershipCandidate> byBinding,
        Map<Long, Set<FormOwnershipCandidate>> byForm
    ) {
        this.byBinding = byBinding;
        this.byForm = byForm;
    }

    static FormOwnershipExpectedIndex create(
        List<FormOwnershipCandidate> expected,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        Map<FormOwnershipBinding, List<FormOwnershipCandidate>> candidatesByBinding =
            groupByBinding(expected);
        Map<FormOwnershipBinding, FormOwnershipCandidate> valid = validCandidates(
            candidatesByBinding,
            drift
        );
        Map<Long, Set<FormOwnershipCandidate>> byForm = groupByForm(valid.values());
        Map<FormOwnershipCoordinate, Set<FormOwnershipCandidate>> byCoordinate =
            groupByCoordinate(valid.values());
        addOwnershipConflicts(byForm, byCoordinate, drift);
        return new FormOwnershipExpectedIndex(valid, byForm);
    }

    Collection<FormOwnershipCandidate> candidates() {
        return byBinding.values();
    }

    boolean contains(FormOwnershipRegistryRow row) {
        return byBinding.containsKey(FormOwnershipBinding.from(row));
    }

    boolean containsForm(long formId) {
        return byForm.containsKey(formId);
    }

    Set<FormOwnershipCandidate> candidatesForForm(long formId) {
        return byForm.getOrDefault(formId, Set.of());
    }

    private static Map<FormOwnershipBinding, List<FormOwnershipCandidate>> groupByBinding(
        List<FormOwnershipCandidate> candidates
    ) {
        Map<FormOwnershipBinding, List<FormOwnershipCandidate>> grouped = new LinkedHashMap<>();
        candidates.forEach(candidate -> grouped
            .computeIfAbsent(FormOwnershipBinding.from(candidate), ignored -> new ArrayList<>())
            .add(candidate));
        return grouped;
    }

    private static Map<FormOwnershipBinding, FormOwnershipCandidate> validCandidates(
        Map<FormOwnershipBinding, List<FormOwnershipCandidate>> candidatesByBinding,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        Map<FormOwnershipBinding, FormOwnershipCandidate> valid = new LinkedHashMap<>();
        candidatesByBinding.forEach((binding, candidates) -> {
            FormOwnershipCandidate candidate = candidates.getFirst();
            if (candidates.size() > 1) {
                drift.get(candidate.sourceName()).add(
                    RegistryDriftType.DUPLICATE_REFERENCE,
                    candidate.binding()
                );
            }
            if (isValid(candidate)) {
                valid.put(binding, candidate);
            } else {
                drift.get(candidate.sourceName()).add(
                    RegistryDriftType.INVALID_REFERENCE,
                    candidate.binding()
                );
            }
        });
        return valid;
    }

    private static Map<Long, Set<FormOwnershipCandidate>> groupByForm(
        Iterable<FormOwnershipCandidate> candidates
    ) {
        Map<Long, Set<FormOwnershipCandidate>> grouped = new HashMap<>();
        candidates.forEach(candidate -> grouped
            .computeIfAbsent(candidate.formId(), ignored -> new HashSet<>())
            .add(candidate));
        return grouped;
    }

    private static Map<FormOwnershipCoordinate, Set<FormOwnershipCandidate>> groupByCoordinate(
        Iterable<FormOwnershipCandidate> candidates
    ) {
        Map<FormOwnershipCoordinate, Set<FormOwnershipCandidate>> grouped = new HashMap<>();
        candidates.forEach(candidate -> grouped
            .computeIfAbsent(FormOwnershipCoordinate.from(candidate), ignored -> new HashSet<>())
            .add(candidate));
        return grouped;
    }

    private static void addOwnershipConflicts(
        Map<Long, Set<FormOwnershipCandidate>> byForm,
        Map<FormOwnershipCoordinate, Set<FormOwnershipCandidate>> byCoordinate,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        byForm.values().stream()
            .filter(candidates -> candidates.stream()
                .map(FormOwnershipCoordinate::from)
                .distinct()
                .count() > 1)
            .flatMap(Set::stream)
            .forEach(candidate -> addConflict(candidate, drift));
        byCoordinate.values().stream()
            .filter(candidates -> candidates.stream()
                .map(FormOwnershipCandidate::formId)
                .distinct()
                .count() > 1)
            .flatMap(Set::stream)
            .forEach(candidate -> addConflict(candidate, drift));
    }

    private static void addConflict(
        FormOwnershipCandidate candidate,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        drift.get(candidate.sourceName()).add(
            RegistryDriftType.OWNERSHIP_CONFLICT,
            candidate.binding()
        );
    }

    private static boolean isValid(FormOwnershipCandidate candidate) {
        try {
            FormOwnerReference.of(
                candidate.formId(),
                candidate.namespace(),
                candidate.ownerResourceKey(),
                candidate.slot()
            );
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
