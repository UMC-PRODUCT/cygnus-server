package com.umc.product.authorization.domain.policy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record PolicyEvaluationFailure(
        PolicyEvaluationFailureCode failureCode,
        Optional<String> statementId,
        Optional<String> attributeName,
        Optional<String> outcomeKey,
        Instant evaluatedAt,
        String schemaVersion,
        String contextSchemaVersion,
        String policyVersion,
        String policyFingerprint)
        implements PolicyEvaluationResult {

    public PolicyEvaluationFailure {
        Objects.requireNonNull(failureCode);
        statementId = Objects.requireNonNull(statementId);
        attributeName = Objects.requireNonNull(attributeName);
        outcomeKey = Objects.requireNonNull(outcomeKey);
        Objects.requireNonNull(evaluatedAt);
        Objects.requireNonNull(schemaVersion);
        Objects.requireNonNull(contextSchemaVersion);
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(policyFingerprint);
    }
}
