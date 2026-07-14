package com.umc.product.authorization.application.service.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.PolicyCombiningAlgorithm;
import com.umc.product.authorization.domain.policy.PolicyEffect;

final class PolicyFingerprintCalculator {

    private final PolicyCanonicalJsonWriter writer = new PolicyCanonicalJsonWriter();

    String calculate(
            String schemaVersion,
            String contextSchemaVersion,
            String namespace,
            String policyVersion,
            PolicyEffect defaultEffect,
            PolicyCombiningAlgorithm combiningAlgorithm,
            List<CompiledPolicyModule> modules) {
        String canonicalJson = writer.writeBundle(
                schemaVersion,
                contextSchemaVersion,
                namespace,
                policyVersion,
                defaultEffect,
                combiningAlgorithm,
                modules);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
