package com.umc.product.authorization.domain.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
        List<CompiledPolicyModule> modules,
        Map<String, List<CompiledPolicyStatement>> statementsByAction) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public CompiledPolicyBundle(
            String schemaVersion,
            String contextSchemaVersion,
            String namespace,
            String policyVersion,
            PolicyEffect defaultEffect,
            PolicyCombiningAlgorithm combiningAlgorithm,
            PolicyDomainSchema domainSchema,
            String policyFingerprint,
            List<CompiledPolicyModule> modules) {
        this(
                schemaVersion,
                contextSchemaVersion,
                namespace,
                policyVersion,
                defaultEffect,
                combiningAlgorithm,
                domainSchema,
                policyFingerprint,
                modules,
                indexByAction(modules));
    }

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
        Map<String, List<CompiledPolicyStatement>> expectedIndex = indexByAction(modules);
        if (!expectedIndex.equals(Objects.requireNonNull(statementsByAction))) {
            throw new IllegalArgumentException("Compiled policy action index must match modules");
        }
        statementsByAction = expectedIndex;
    }

    public List<CompiledPolicyStatement> statementsForAction(String actionId) {
        return statementsByAction.getOrDefault(Objects.requireNonNull(actionId), List.of());
    }

    private static Map<String, List<CompiledPolicyStatement>> indexByAction(
            List<CompiledPolicyModule> modules) {
        Objects.requireNonNull(modules);
        Map<String, List<CompiledPolicyStatement>> mutable = new TreeMap<>();
        for (CompiledPolicyModule module : modules) {
            for (CompiledPolicyStatement statement : Objects.requireNonNull(module).statements()) {
                for (String action : statement.actions()) {
                    mutable.computeIfAbsent(action, ignored -> new ArrayList<>()).add(statement);
                }
            }
        }
        Map<String, List<CompiledPolicyStatement>> immutable = new TreeMap<>();
        mutable.forEach((action, statements) -> immutable.put(action, List.copyOf(statements)));
        return Collections.unmodifiableMap(immutable);
    }
}
