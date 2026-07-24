package com.umc.product.authorization.domain.policy.rollout;

import java.util.List;
import java.util.Objects;

public record PolicyEnforcementReceiptDocument(
    String schemaVersion,
    String namespace,
    List<PolicyEnforcementReceipt> receipts
) {

    public PolicyEnforcementReceiptDocument {
        Objects.requireNonNull(schemaVersion);
        Objects.requireNonNull(namespace);
        receipts = List.copyOf(receipts);
    }
}
