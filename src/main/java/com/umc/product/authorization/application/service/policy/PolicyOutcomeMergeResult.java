package com.umc.product.authorization.application.service.policy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;

sealed interface PolicyOutcomeMergeResult
        permits PolicyOutcomeMergeResult.Success, PolicyOutcomeMergeResult.Failure {

    record Success(List<PolicyResolvedOutcome> outcomes) implements PolicyOutcomeMergeResult {
        public Success {
            outcomes = List.copyOf(outcomes);
        }
    }

    record Failure(
            PolicyEvaluationFailureCode code,
            Optional<String> statementId,
            Optional<String> attributeName,
            Optional<String> outcomeKey)
            implements PolicyOutcomeMergeResult {
        public Failure {
            Objects.requireNonNull(code);
            statementId = Objects.requireNonNull(statementId);
            attributeName = Objects.requireNonNull(attributeName);
            outcomeKey = Objects.requireNonNull(outcomeKey);
        }
    }
}
