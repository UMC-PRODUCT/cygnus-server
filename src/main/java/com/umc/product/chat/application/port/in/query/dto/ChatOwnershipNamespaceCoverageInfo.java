package com.umc.product.chat.application.port.in.query.dto;

import java.util.List;

public record ChatOwnershipNamespaceCoverageInfo(
    List<String> persistedNamespaces,
    List<String> declaredNamespaces,
    List<String> evaluatorNamespaces
) {

    public ChatOwnershipNamespaceCoverageInfo {
        persistedNamespaces = List.copyOf(persistedNamespaces);
        declaredNamespaces = List.copyOf(declaredNamespaces);
        evaluatorNamespaces = List.copyOf(evaluatorNamespaces);
    }
}
