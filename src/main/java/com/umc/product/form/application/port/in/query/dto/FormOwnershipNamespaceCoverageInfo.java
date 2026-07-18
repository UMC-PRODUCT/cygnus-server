package com.umc.product.form.application.port.in.query.dto;

import java.util.List;

public record FormOwnershipNamespaceCoverageInfo(
    List<String> persistedNamespaces,
    List<String> declaredNamespaces,
    List<String> evaluatorNamespaces
) {

    public FormOwnershipNamespaceCoverageInfo {
        persistedNamespaces = List.copyOf(persistedNamespaces);
        declaredNamespaces = List.copyOf(declaredNamespaces);
        evaluatorNamespaces = List.copyOf(evaluatorNamespaces);
    }
}
