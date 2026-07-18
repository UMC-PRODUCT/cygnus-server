package com.umc.product.registry.application.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.application.port.out.RegistryNamespaceCoveragePort;
import com.umc.product.registry.application.port.out.RegistryStateQueryPort;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;
import com.umc.product.registry.domain.RegistryStatus;

@Service
public class RegistryReadinessService implements GetRegistryReadinessUseCase {

    private final RegistryStateQueryPort stateQueryPort;
    private final Map<RegistryName, List<RegistryNamespaceCoveragePort>> coverageByRegistry;
    private final EngineOwnershipProperties properties;

    public RegistryReadinessService(
        RegistryStateQueryPort stateQueryPort,
        List<RegistryNamespaceCoveragePort> coveragePorts,
        EngineOwnershipProperties properties
    ) {
        this.stateQueryPort = stateQueryPort;
        this.coverageByRegistry = indexCoverage(coveragePorts);
        this.properties = properties;
    }

    @Override
    public boolean isReady(RegistryName registryName) {
        if (!isDatabaseReady(registryName)) {
            return false;
        }
        return registryName == RegistryName.STORAGE_USAGE
            || invalidNamespaces(registryName).isEmpty();
    }

    @Override
    public boolean areAllRegistriesReady() {
        for (RegistryName registryName : RegistryName.values()) {
            if (!isReady(registryName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> invalidNamespaces(RegistryName registryName) {
        List<RegistryNamespaceCoveragePort> contributions = coverageByRegistry.getOrDefault(
            registryName,
            List.of()
        );
        if (registryName != RegistryName.STORAGE_USAGE && contributions.isEmpty()) {
            return List.of("<coverage-missing>");
        }
        Set<String> required = new HashSet<>();
        Map<String, Integer> evaluatorCounts = new HashMap<>();
        boolean malformed = false;
        for (RegistryNamespaceCoveragePort contribution : contributions) {
            RegistryNamespaceCoverage coverage;
            try {
                coverage = contribution.loadCoverage();
            } catch (RuntimeException exception) {
                malformed = true;
                continue;
            }
            if (coverage == null) {
                malformed = true;
                continue;
            }
            malformed |= coverage.evaluatorNamespaces() == null;
            malformed |= addValid(required, coverage.persistedNamespaces());
            malformed |= addValid(required, coverage.declaredNamespaces());
            for (String namespace : safe(coverage.evaluatorNamespaces())) {
                if (namespace == null || namespace.isBlank()) {
                    malformed = true;
                } else {
                    evaluatorCounts.merge(namespace, 1, Integer::sum);
                }
            }
        }
        List<String> invalid = new ArrayList<>(required.stream()
            .filter(namespace -> evaluatorCounts.getOrDefault(namespace, 0) != 1)
            .sorted()
            .toList());
        if (malformed) {
            invalid.add("<coverage-invalid>");
        }
        if (required.isEmpty()) {
            invalid.add("<coverage-empty>");
        }
        return List.copyOf(invalid);
    }

    @Override
    public OwnershipEnforcementMode ownershipMode(RegistryName registryName) {
        if (registryName == RegistryName.STORAGE_USAGE) {
            throw new IllegalArgumentException("ownership registry만 enforcement mode를 가집니다.");
        }
        if (!properties.enforcementEnabled()) {
            return OwnershipEnforcementMode.AUDIT;
        }
        return isReady(registryName)
            ? OwnershipEnforcementMode.STRICT
            : OwnershipEnforcementMode.BLOCKED;
    }

    private boolean isDatabaseReady(RegistryName registryName) {
        try {
            return stateQueryPort.loadState(registryName.canonicalName()).status()
                == RegistryStatus.READY;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Map<RegistryName, List<RegistryNamespaceCoveragePort>> indexCoverage(
        List<RegistryNamespaceCoveragePort> coveragePorts
    ) {
        Map<RegistryName, List<RegistryNamespaceCoveragePort>> indexed =
            new EnumMap<>(RegistryName.class);
        for (RegistryNamespaceCoveragePort coveragePort : safe(coveragePorts)) {
            if (coveragePort == null || coveragePort.registryName() == null) {
                continue;
            }
            indexed.computeIfAbsent(coveragePort.registryName(), ignored -> new ArrayList<>())
                .add(coveragePort);
        }
        return indexed;
    }

    private static boolean addValid(Set<String> destination, List<String> namespaces) {
        boolean malformed = namespaces == null;
        for (String namespace : safe(namespaces)) {
            if (namespace == null || namespace.isBlank()) {
                malformed = true;
            } else {
                destination.add(namespace);
            }
        }
        return malformed;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
