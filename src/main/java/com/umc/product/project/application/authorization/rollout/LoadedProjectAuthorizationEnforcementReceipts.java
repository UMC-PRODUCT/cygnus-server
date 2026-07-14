package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public record LoadedProjectAuthorizationEnforcementReceipts(
    ProjectAuthorizationEnforcementReceiptDocument document,
    String artifactSha256
) {
    public LoadedProjectAuthorizationEnforcementReceipts {
        Objects.requireNonNull(document);
        Objects.requireNonNull(artifactSha256);
    }
}
