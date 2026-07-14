package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record CompiledPolicyBundle(
        String schemaVersion,
        String contextSchemaVersion,
        String namespace,
        String policyVersion,
        PolicyEffect defaultEffect,
        PolicyCombiningAlgorithm combiningAlgorithm,
        PolicyDomainSchema domainSchema,
        String policyFingerprint,
        List<CompiledPolicyModule> modules) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public CompiledPolicyBundle {
        Objects.requireNonNull(schemaVersion);
        Objects.requireNonNull(contextSchemaVersion);
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(defaultEffect);
        Objects.requireNonNull(combiningAlgorithm);
        Objects.requireNonNull(domainSchema);
        Objects.requireNonNull(policyFingerprint);
        if (!contextSchemaVersion.equals(domainSchema.contextSchemaVersion())) {
            throw new IllegalArgumentException("Compiled policy domain schema version must match");
        }
        if (!SHA_256.matcher(policyFingerprint).matches()) {
            throw new IllegalArgumentException("Policy fingerprint must be a lowercase SHA-256");
        }
        modules = List.copyOf(modules);
    }
}
