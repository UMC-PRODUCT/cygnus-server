package com.umc.product.project.application.authorization.rollout;

import java.util.List;
import java.util.Objects;

public record ProjectAuthorizationEnforcementReceiptDocument(
    String schemaVersion,
    List<ProjectAuthorizationEnforcementReceipt> receipts
) {
    public ProjectAuthorizationEnforcementReceiptDocument {
        Objects.requireNonNull(schemaVersion);
        receipts = List.copyOf(receipts);
    }

    public static ProjectAuthorizationEnforcementReceiptDocument empty() {
        return new ProjectAuthorizationEnforcementReceiptDocument("1.0", List.of());
    }
}
