package com.umc.product.project.application.authorization;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyOperator;
import com.umc.product.authorization.domain.policy.PolicyValue;

final class ProjectPolicyStatementWitnessFactory {

    private final PolicyDomainSchema schema;

    ProjectPolicyStatementWitnessFactory(PolicyDomainSchema schema) {
        this.schema = schema;
    }

    Optional<PolicyAttributeSet> matching(String actionId, PolicyCondition condition) {
        IdentityHashMap<PolicyCondition.Predicate, Boolean> desired = new IdentityHashMap<>();
        assignTruth(condition, true, desired);
        return attributes(actionId, condition, desired);
    }

    Optional<MutationWitness> mutation(
        String actionId,
        PolicyCondition condition,
        PolicyCondition.Predicate target
    ) {
        IdentityHashMap<PolicyCondition.Predicate, Boolean> matching = new IdentityHashMap<>();
        if (!assignCritical(condition, target, matching)) {
            return Optional.empty();
        }
        Optional<PolicyAttributeSet> matchingAttributes = attributes(actionId, condition, matching)
            .or(() -> searchAttributes(actionId, condition, target, true, true));
        IdentityHashMap<PolicyCondition.Predicate, Boolean> mutated = new IdentityHashMap<>(matching);
        mutated.put(target, false);
        Optional<PolicyAttributeSet> mutatedAttributes = attributes(actionId, condition, mutated)
            .or(() -> searchAttributes(actionId, condition, target, false, false));
        if (matchingAttributes.isEmpty() || mutatedAttributes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MutationWitness(matchingAttributes.orElseThrow(), mutatedAttributes.orElseThrow()));
    }

    List<PolicyCondition.Predicate> leaves(PolicyCondition condition) {
        List<PolicyCondition.Predicate> result = new ArrayList<>();
        collectLeaves(condition, result);
        return List.copyOf(result);
    }

    int anyNodeCount(PolicyCondition condition) {
        if (condition instanceof PolicyCondition.Predicate) {
            return 0;
        }
        List<PolicyCondition> children = children(condition);
        int current = condition instanceof PolicyCondition.Any ? 1 : 0;
        return current + children.stream().mapToInt(this::anyNodeCount).sum();
    }

    private Optional<PolicyAttributeSet> attributes(
        String actionId,
        PolicyCondition condition,
        IdentityHashMap<PolicyCondition.Predicate, Boolean> desired
    ) {
        Map<String, PolicyValue> conditionValues = new LinkedHashMap<>();
        for (PolicyCondition.Predicate predicate : leaves(condition)) {
            Optional<AttributeValue> assignment = assignment(predicate, desired.get(predicate));
            if (assignment.isEmpty()) {
                return Optional.empty();
            }
            AttributeValue value = assignment.orElseThrow();
            PolicyValue previous = conditionValues.putIfAbsent(value.name(), value.value());
            if (previous != null && !previous.equals(value.value())) {
                return Optional.empty();
            }
        }
        return Optional.of(buildAttributes(actionId, conditionValues));
    }

    private Optional<PolicyAttributeSet> searchAttributes(
        String actionId,
        PolicyCondition condition,
        PolicyCondition.Predicate target,
        boolean targetTruth,
        boolean conditionTruth
    ) {
        Map<String, List<PolicyValue>> candidates = new LinkedHashMap<>();
        for (PolicyCondition.Predicate predicate : leaves(condition)) {
            for (boolean desired : List.of(true, false)) {
                assignment(predicate, desired).ifPresent(value -> candidates
                    .computeIfAbsent(value.name(), ignored -> new ArrayList<>())
                    .add(value.value()));
            }
        }
        candidates.values().forEach(values -> {
            List<PolicyValue> distinct = values.stream().distinct().toList();
            values.clear();
            values.addAll(distinct);
        });
        return searchAttributes(
            actionId,
            condition,
            target,
            targetTruth,
            conditionTruth,
            new ArrayList<>(candidates.entrySet()),
            0,
            new LinkedHashMap<>()
        );
    }

    private Optional<PolicyAttributeSet> searchAttributes(
        String actionId,
        PolicyCondition condition,
        PolicyCondition.Predicate target,
        boolean targetTruth,
        boolean conditionTruth,
        List<Map.Entry<String, List<PolicyValue>>> candidates,
        int index,
        Map<String, PolicyValue> values
    ) {
        if (index == candidates.size()) {
            if (matches(target, values) == targetTruth && matches(condition, values) == conditionTruth) {
                return Optional.of(buildAttributes(actionId, values));
            }
            return Optional.empty();
        }
        Map.Entry<String, List<PolicyValue>> candidate = candidates.get(index);
        for (PolicyValue value : candidate.getValue()) {
            values.put(candidate.getKey(), value);
            Optional<PolicyAttributeSet> result = searchAttributes(
                actionId,
                condition,
                target,
                targetTruth,
                conditionTruth,
                candidates,
                index + 1,
                values
            );
            if (result.isPresent()) {
                return result;
            }
        }
        values.remove(candidate.getKey());
        return Optional.empty();
    }

    private PolicyAttributeSet buildAttributes(String actionId, Map<String, PolicyValue> conditionValues) {
        ActionSchema action = schema.action(actionId).orElseThrow();
        Map<String, PolicyValue> values = new LinkedHashMap<>();
        action.requiredAttributes().stream().sorted()
            .forEach(name -> values.put(name, ProjectPolicyTestValues.defaultValue(attribute(name))));
        values.putAll(conditionValues);

        PolicyAttributeSet.Builder builder = PolicyAttributeSet.builder();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            AttributeSchema registered = attribute(entry.getKey());
            PolicyAttributeKey<PolicyValue> key =
                new PolicyAttributeKey<>(registered.attributeName(), registered.type());
            builder.put(key, entry.getValue());
        });
        return builder.build();
    }

    private boolean matches(PolicyCondition condition, Map<String, PolicyValue> values) {
        if (condition instanceof PolicyCondition.Predicate predicate) {
            return matches(predicate, values);
        }
        if (condition instanceof PolicyCondition.All all) {
            return all.children().stream().allMatch(child -> matches(child, values));
        }
        return ((PolicyCondition.Any) condition).children().stream().anyMatch(child -> matches(child, values));
    }

    private boolean matches(PolicyCondition.Predicate predicate, Map<String, PolicyValue> values) {
        PolicyOperand.Attribute attribute = (PolicyOperand.Attribute) predicate.operands().get(0);
        PolicyOperand.Literal literal = (PolicyOperand.Literal) predicate.operands().get(1);
        PolicyValue actual = values.get(attribute.name());
        if (predicate.operator() == PolicyOperator.EQ) {
            return literal.value().equals(actual);
        }
        return contains(literal.value(), actual);
    }

    private boolean contains(PolicyValue set, PolicyValue value) {
        if (set instanceof PolicyValue.LongSetValue values && value instanceof PolicyValue.LongValue scalar) {
            return values.value().contains(scalar.value());
        }
        if (set instanceof PolicyValue.StringSetValue values && value instanceof PolicyValue.StringValue scalar) {
            return values.value().contains(scalar.value());
        }
        if (set instanceof PolicyValue.EnumSetValue values && value instanceof PolicyValue.EnumValue scalar) {
            return values.value().contains(scalar.value());
        }
        if (set instanceof PolicyValue.InstantSetValue values && value instanceof PolicyValue.InstantValue scalar) {
            return values.value().contains(scalar.value());
        }
        return false;
    }

    private Optional<AttributeValue> assignment(PolicyCondition.Predicate predicate, Boolean desired) {
        if (desired == null || predicate.operands().size() != 2) {
            return Optional.empty();
        }
        if (!(predicate.operands().get(0) instanceof PolicyOperand.Attribute attribute)
            || !(predicate.operands().get(1) instanceof PolicyOperand.Literal literal)) {
            return Optional.empty();
        }
        AttributeSchema registered = attribute(attribute.name());
        if (predicate.operator() == PolicyOperator.EQ) {
            PolicyValue value = desired
                ? literal.value()
                : ProjectPolicyTestValues.differentValue(registered, literal.value()).orElse(null);
            return value == null ? Optional.empty() : Optional.of(new AttributeValue(attribute.name(), value));
        }
        if (predicate.operator() == PolicyOperator.IN) {
            Optional<PolicyValue> value = desired
                ? ProjectPolicyTestValues.firstElement(literal.value())
                : ProjectPolicyTestValues.outsideElement(registered, literal.value());
            return value.map(policyValue -> new AttributeValue(attribute.name(), policyValue));
        }
        return Optional.empty();
    }

    private AttributeSchema attribute(String name) {
        return schema.attribute(name).orElseThrow();
    }

    private boolean assignCritical(
        PolicyCondition condition,
        PolicyCondition.Predicate target,
        IdentityHashMap<PolicyCondition.Predicate, Boolean> desired
    ) {
        if (condition instanceof PolicyCondition.Predicate predicate) {
            if (predicate != target) {
                return false;
            }
            desired.put(predicate, true);
            return true;
        }
        List<PolicyCondition> children = children(condition);
        Optional<PolicyCondition> targetChild = children.stream().filter(child -> contains(child, target)).findFirst();
        if (targetChild.isEmpty()) {
            return false;
        }
        boolean siblingTruth = condition instanceof PolicyCondition.All;
        for (PolicyCondition child : children) {
            if (child == targetChild.orElseThrow()) {
                assignCritical(child, target, desired);
            } else {
                assignTruth(child, siblingTruth, desired);
            }
        }
        return true;
    }

    private void assignTruth(
        PolicyCondition condition,
        boolean desiredTruth,
        IdentityHashMap<PolicyCondition.Predicate, Boolean> desired
    ) {
        if (condition instanceof PolicyCondition.Predicate predicate) {
            desired.put(predicate, desiredTruth);
            return;
        }
        List<PolicyCondition> children = children(condition);
        if (condition instanceof PolicyCondition.All) {
            for (int index = 0; index < children.size(); index++) {
                assignTruth(children.get(index), desiredTruth || index > 0, desired);
            }
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            assignTruth(children.get(index), desiredTruth && index == 0, desired);
        }
    }

    private boolean contains(PolicyCondition condition, PolicyCondition.Predicate target) {
        if (condition instanceof PolicyCondition.Predicate predicate) {
            return predicate == target;
        }
        return children(condition).stream().anyMatch(child -> contains(child, target));
    }

    private void collectLeaves(PolicyCondition condition, List<PolicyCondition.Predicate> result) {
        if (condition instanceof PolicyCondition.Predicate predicate) {
            result.add(predicate);
            return;
        }
        children(condition).forEach(child -> collectLeaves(child, result));
    }

    private List<PolicyCondition> children(PolicyCondition condition) {
        if (condition instanceof PolicyCondition.All all) {
            return all.children();
        }
        return ((PolicyCondition.Any) condition).children();
    }

    record MutationWitness(PolicyAttributeSet matching, PolicyAttributeSet mutated) {}

    private record AttributeValue(String name, PolicyValue value) {}
}
