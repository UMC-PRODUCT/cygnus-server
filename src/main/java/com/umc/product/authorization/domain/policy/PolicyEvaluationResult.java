package com.umc.product.authorization.domain.policy;

import java.time.Instant;

public sealed interface PolicyEvaluationResult permits PolicyDecision, PolicyEvaluationFailure {

    Instant evaluatedAt();

    String schemaVersion();

    String contextSchemaVersion();

    String policyVersion();

    String policyFingerprint();
}
