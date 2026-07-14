package com.umc.product.authorization.domain.policy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PolicyDecision(
        PolicyEffect effect,
        List<String> matchedAllowStatementIds,
        List<String> matchedDenyStatementIds,
        List<PolicyResolvedOutcome> outcomes,
        Instant evaluatedAt,
        String schemaVersion,
        String contextSchemaVersion,
        String policyVersion,
        String policyFingerprint)
        implements PolicyEvaluationResult {

    public PolicyDecision {
        Objects.requireNonNull(effect);
        matchedAllowStatementIds = sortedCopy(matchedAllowStatementIds);
        matchedDenyStatementIds = sortedCopy(matchedDenyStatementIds);
        outcomes = sortedOutcomes(outcomes);
        Objects.requireNonNull(evaluatedAt);
        Objects.requireNonNull(schemaVersion);
        Objects.requireNonNull(contextSchemaVersion);
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(policyFingerprint);
    }

    public Optional<PolicyValue> outcome(String key) {
        return outcomes.stream()
                .filter(outcome -> outcome.key().equals(key))
                .map(PolicyResolvedOutcome::value)
                .findFirst();
    }

    private static List<String> sortedCopy(List<String> values) {
        Objects.requireNonNull(values);
        return values.stream().map(Objects::requireNonNull).sorted().toList();
    }

    private static List<PolicyResolvedOutcome> sortedOutcomes(List<PolicyResolvedOutcome> values) {
        Objects.requireNonNull(values);
        List<PolicyResolvedOutcome> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(PolicyResolvedOutcome::key));
        for (int index = 1; index < sorted.size(); index++) {
            if (sorted.get(index - 1).key().equals(sorted.get(index).key())) {
                throw new IllegalArgumentException("Duplicate resolved policy outcome");
            }
        }
        return List.copyOf(sorted);
    }
}
