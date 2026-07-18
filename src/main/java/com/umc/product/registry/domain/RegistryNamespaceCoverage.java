package com.umc.product.registry.domain;

import java.util.List;

public record RegistryNamespaceCoverage(
    List<String> persistedNamespaces,
    List<String> declaredNamespaces,
    List<String> evaluatorNamespaces
) {

    public RegistryNamespaceCoverage {
        persistedNamespaces = persistedNamespaces == null
            ? null
            : List.copyOf(persistedNamespaces);
        declaredNamespaces = declaredNamespaces == null
            ? null
            : List.copyOf(declaredNamespaces);
        evaluatorNamespaces = evaluatorNamespaces == null
            ? null
            : List.copyOf(evaluatorNamespaces);
    }
}
