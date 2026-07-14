package com.umc.product.authorization.application.service.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

final class PolicyOutcomeMerger {

    PolicyOutcomeMergeResult merge(
            List<CompiledPolicyStatement> statements,
            PolicyAttributeSet attributes,
            PolicyDomainSchema domainSchema) {
        Map<String, List<Emission>> byKey = new TreeMap<>();
        List<CompiledPolicyStatement> sortedStatements = statements.stream()
                .sorted(Comparator.comparing(CompiledPolicyStatement::id))
                .toList();
        for (CompiledPolicyStatement statement : sortedStatements) {
            for (CompiledPolicyOutcome outcome : statement.outcomes()) {
                Optional<PolicyValue> resolved = resolve(outcome.value(), attributes);
                if (resolved.isEmpty()) {
                    PolicyOperand.Attribute attribute = (PolicyOperand.Attribute) outcome.value();
                    return failure(
                            PolicyEvaluationFailureCode.OUTCOME_ATTRIBUTE_MISSING,
                            Optional.of(statement.id()),
                            Optional.of(attribute.name()),
                            Optional.of(outcome.key()));
                }
                byKey.computeIfAbsent(outcome.key(), ignored -> new ArrayList<>())
                        .add(new Emission(statement.id(), resolved.orElseThrow()));
            }
        }

        List<PolicyResolvedOutcome> outcomes = new ArrayList<>();
        for (Map.Entry<String, List<Emission>> entry : byKey.entrySet()) {
            OutcomeSchema schema = domainSchema.outcome(entry.getKey()).orElseThrow();
            PolicyOutcomeMergeResult merged = mergeOne(schema, entry.getValue());
            if (merged instanceof PolicyOutcomeMergeResult.Failure failure) {
                return failure;
            }
            PolicyOutcomeMergeResult.Success success = (PolicyOutcomeMergeResult.Success) merged;
            outcomes.addAll(success.outcomes());
        }
        return new PolicyOutcomeMergeResult.Success(outcomes);
    }

    private Optional<PolicyValue> resolve(PolicyOperand operand, PolicyAttributeSet attributes) {
        if (operand instanceof PolicyOperand.Attribute attribute) {
            return attributes.value(attribute.name());
        }
        return Optional.of(((PolicyOperand.Literal) operand).value());
    }

    private PolicyOutcomeMergeResult mergeOne(OutcomeSchema schema, List<Emission> emissions) {
        for (Emission emission : emissions) {
            if (emission.value().type() != schema.type()) {
                return failure(
                        PolicyEvaluationFailureCode.OUTCOME_VALUE_UNSUPPORTED,
                        Optional.of(emission.statementId()),
                        Optional.empty(),
                        Optional.of(schema.outcomeKey()));
            }
        }
        return switch (schema.mergeStrategy()) {
            case BOOLEAN_OR -> success(schema.outcomeKey(), mergeBoolean(emissions));
            case SET_UNION -> success(schema.outcomeKey(), mergeSet(schema.type(), emissions));
            case DOMINANCE -> mergeDominance(schema, emissions);
            case EXACTLY_ONE -> mergeExactlyOne(schema.outcomeKey(), emissions);
        };
    }

    private PolicyValue mergeBoolean(List<Emission> emissions) {
        boolean value = emissions.stream()
                .map(Emission::value)
                .map(PolicyValue.BooleanValue.class::cast)
                .anyMatch(PolicyValue.BooleanValue::value);
        return new PolicyValue.BooleanValue(value);
    }

    private PolicyValue mergeSet(PolicyValueType type, List<Emission> emissions) {
        return switch (type) {
            case LONG_SET -> new PolicyValue.LongSetValue(unionLongs(emissions));
            case STRING_SET -> new PolicyValue.StringSetValue(unionStrings(emissions));
            case ENUM_SET -> new PolicyValue.EnumSetValue(unionEnums(emissions));
            case INSTANT_SET -> new PolicyValue.InstantSetValue(unionInstants(emissions));
            default -> throw new IllegalStateException("SET_UNION requires a set outcome type");
        };
    }

    private PolicyOutcomeMergeResult mergeDominance(OutcomeSchema schema, List<Emission> emissions) {
        Emission winner = emissions.getFirst();
        int winnerIndex = schema.dominanceOrder().indexOf(symbol(winner.value()));
        if (winnerIndex < 0) {
            return failure(
                    PolicyEvaluationFailureCode.OUTCOME_VALUE_UNSUPPORTED,
                    Optional.of(winner.statementId()),
                    Optional.empty(),
                    Optional.of(schema.outcomeKey()));
        }
        for (Emission emission : emissions.subList(1, emissions.size())) {
            String symbol = symbol(emission.value());
            int index = schema.dominanceOrder().indexOf(symbol);
            if (index < 0) {
                return failure(
                        PolicyEvaluationFailureCode.OUTCOME_VALUE_UNSUPPORTED,
                        Optional.of(emission.statementId()),
                        Optional.empty(),
                        Optional.of(schema.outcomeKey()));
            }
            if (index < winnerIndex) {
                winner = emission;
                winnerIndex = index;
            }
        }
        return success(schema.outcomeKey(), winner.value());
    }

    private PolicyOutcomeMergeResult mergeExactlyOne(String key, List<Emission> emissions) {
        Emission first = emissions.getFirst();
        for (Emission emission : emissions) {
            if (!first.value().equals(emission.value())) {
                return failure(
                        PolicyEvaluationFailureCode.EXACTLY_ONE_CONFLICT,
                        Optional.of(emission.statementId()),
                        Optional.empty(),
                        Optional.of(key));
            }
        }
        return success(key, first.value());
    }

    private Set<Long> unionLongs(List<Emission> emissions) {
        Set<Long> result = new TreeSet<>();
        emissions.stream()
                .map(Emission::value)
                .map(PolicyValue.LongSetValue.class::cast)
                .map(PolicyValue.LongSetValue::value)
                .forEach(result::addAll);
        return result;
    }

    private Set<String> unionStrings(List<Emission> emissions) {
        Set<String> result = new TreeSet<>();
        emissions.stream()
                .map(Emission::value)
                .map(PolicyValue.StringSetValue.class::cast)
                .map(PolicyValue.StringSetValue::value)
                .forEach(result::addAll);
        return result;
    }

    private Set<String> unionEnums(List<Emission> emissions) {
        Set<String> result = new TreeSet<>();
        emissions.stream()
                .map(Emission::value)
                .map(PolicyValue.EnumSetValue.class::cast)
                .map(PolicyValue.EnumSetValue::value)
                .forEach(result::addAll);
        return result;
    }

    private Set<java.time.Instant> unionInstants(List<Emission> emissions) {
        Set<java.time.Instant> result = new TreeSet<>();
        emissions.stream()
                .map(Emission::value)
                .map(PolicyValue.InstantSetValue.class::cast)
                .map(PolicyValue.InstantSetValue::value)
                .forEach(result::addAll);
        return result;
    }

    private String symbol(PolicyValue value) {
        if (value instanceof PolicyValue.EnumValue enumValue) {
            return enumValue.value();
        }
        return ((PolicyValue.StringValue) value).value();
    }

    private PolicyOutcomeMergeResult success(String key, PolicyValue value) {
        return new PolicyOutcomeMergeResult.Success(List.of(new PolicyResolvedOutcome(key, value)));
    }

    private PolicyOutcomeMergeResult failure(
            PolicyEvaluationFailureCode code,
            Optional<String> statementId,
            Optional<String> attributeName,
            Optional<String> outcomeKey) {
        return new PolicyOutcomeMergeResult.Failure(code, statementId, attributeName, outcomeKey);
    }

    private record Emission(String statementId, PolicyValue value) {}
}
