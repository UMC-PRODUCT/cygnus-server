package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record PolicyBundleIdentity(
    String schemaVersion,
    String contextSchemaVersion,
    String policyVersion,
    String policyFingerprint
) {

    public PolicyBundleIdentity {
        Objects.requireNonNull(schemaVersion);
        Objects.requireNonNull(contextSchemaVersion);
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(policyFingerprint);
    }

    public static PolicyBundleIdentity from(CompiledPolicyBundle bundle) {
        Objects.requireNonNull(bundle);
        return new PolicyBundleIdentity(
            bundle.schemaVersion(),
            bundle.contextSchemaVersion(),
            bundle.policyVersion(),
            bundle.policyFingerprint());
    }
}
