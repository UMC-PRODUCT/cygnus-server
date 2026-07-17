package com.umc.product.form.adapter.out.backfill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

final class FormOwnershipReconciler {

    private final JdbcTemplate jdbcTemplate;
    private final List<FormOwnershipBackfillSource> sources;
    private final FormOwnershipMappingLoader mappingLoader;

    FormOwnershipReconciler(
        JdbcTemplate jdbcTemplate,
        List<FormOwnershipBackfillSource> sources,
        FormOwnershipMappingLoader mappingLoader
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sources = sources;
        this.mappingLoader = mappingLoader;
    }

    RegistryReconciliationResult reconcile(int detailLimit) {
        if (detailLimit <= 0) {
            throw new IllegalArgumentException("detail limit은 양수여야 합니다.");
        }
        Map<String, FormOwnershipDriftAccumulator> drift = accumulators(detailLimit);
        Set<Long> formIds = loadFormIds();
        FormOwnershipExpectedIndex expected = FormOwnershipExpectedIndex.create(
            expectedCandidates(formIds),
            drift
        );
        FormOwnershipRegistrySnapshot actual = FormOwnershipRegistrySnapshot.load(jdbcTemplate);
        reconcileExpected(expected, actual, formIds, drift);
        reconcileActual(expected, actual, formIds, drift);
        List<RegistrySourceReconciliation> summaries = drift.values().stream()
            .map(FormOwnershipDriftAccumulator::result)
            .toList();
        return new RegistryReconciliationResult(FormOwnershipRolloutAdapter.REGISTRY_NAME, summaries);
    }

    private Map<String, FormOwnershipDriftAccumulator> accumulators(int detailLimit) {
        Map<String, FormOwnershipDriftAccumulator> drift = new LinkedHashMap<>();
        sources.forEach(source -> drift.put(
            source.sourceName(),
            new FormOwnershipDriftAccumulator(source.sourceName(), detailLimit)
        ));
        drift.put(
            FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME,
            new FormOwnershipDriftAccumulator(
                FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME,
                detailLimit
            )
        );
        return drift;
    }

    private Set<Long> loadFormIds() {
        return new TreeSet<>(jdbcTemplate.queryForList(
            "SELECT id FROM form ORDER BY id",
            Long.class
        ));
    }

    private List<FormOwnershipCandidate> expectedCandidates(Set<Long> formIds) {
        List<FormOwnershipCandidate> named = mappingLoader.loadAll(sources);
        Set<Long> namedFormIds = new HashSet<>();
        named.forEach(candidate -> namedFormIds.add(candidate.formId()));
        List<FormOwnershipCandidate> expected = new ArrayList<>(named);
        formIds.stream()
            .filter(formId -> !namedFormIds.contains(formId))
            .map(FormOwnershipRolloutAdapter::standaloneCandidate)
            .forEach(expected::add);
        return expected;
    }

    private void reconcileExpected(
        FormOwnershipExpectedIndex expected,
        FormOwnershipRegistrySnapshot actual,
        Set<Long> formIds,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        expected.candidates().forEach(candidate -> {
            FormOwnershipDriftAccumulator sourceDrift = drift.get(candidate.sourceName());
            if (!formIds.contains(candidate.formId())) {
                sourceDrift.add(RegistryDriftType.BROKEN_REFERENCE, candidate.binding());
            } else if (!actual.contains(candidate)) {
                sourceDrift.add(RegistryDriftType.SOURCE_ONLY_MISSING, candidate.binding());
                if (actual.containsForm(candidate.formId()) || actual.containsCoordinate(candidate)) {
                    sourceDrift.add(RegistryDriftType.OWNERSHIP_CONFLICT, candidate.binding());
                }
            }
        });
    }

    private void reconcileActual(
        FormOwnershipExpectedIndex expected,
        FormOwnershipRegistrySnapshot actual,
        Set<Long> formIds,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        for (FormOwnershipRegistryRow row : actual.rows()) {
            if (expected.contains(row)) {
                continue;
            }
            if (!formIds.contains(row.formId())) {
                addStale(row, drift);
            } else if (expected.containsForm(row.formId())) {
                expected.candidatesForForm(row.formId()).forEach(candidate -> drift
                    .get(candidate.sourceName())
                    .add(RegistryDriftType.OWNERSHIP_CONFLICT, candidate.binding()));
            } else {
                addStale(row, drift);
            }
        }
    }

    private void addStale(
        FormOwnershipRegistryRow row,
        Map<String, FormOwnershipDriftAccumulator> drift
    ) {
        drift.get(sourceNameFor(row)).add(RegistryDriftType.REGISTRY_ONLY_STALE, row.display());
    }

    private String sourceNameFor(FormOwnershipRegistryRow row) {
        return sources.stream()
            .filter(source -> source.namespace().equals(row.namespace()))
            .filter(source -> source.slot().equals(row.slot()))
            .map(FormOwnershipBackfillSource::sourceName)
            .findFirst()
            .orElse(FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME);
    }
}
