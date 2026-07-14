package com.umc.product.authorization.application.service.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyOperator;
import com.umc.product.authorization.domain.policy.PolicyValue;

final class PolicyConditionEvaluator {

    boolean evaluate(PolicyCondition condition, PolicyAttributeSet attributes) {
        if (condition instanceof PolicyCondition.All all) {
            return all.children().stream().allMatch(child -> evaluate(child, attributes));
        }
        if (condition instanceof PolicyCondition.Any any) {
            return any.children().stream().anyMatch(child -> evaluate(child, attributes));
        }
        return evaluatePredicate((PolicyCondition.Predicate) condition, attributes);
    }

    private boolean evaluatePredicate(PolicyCondition.Predicate predicate, PolicyAttributeSet attributes) {
        PolicyOperator operator = predicate.operator();
        if (operator == PolicyOperator.EXISTS || operator == PolicyOperator.NOT_EXISTS) {
            PolicyOperand.Attribute attribute = (PolicyOperand.Attribute) predicate.operands().getFirst();
            boolean present = attributes.contains(attribute.name());
            return operator == PolicyOperator.EXISTS ? present : !present;
        }

        List<PolicyValue> values = resolve(predicate.operands(), attributes);
        if (values.isEmpty()) {
            return false;
        }
        PolicyValue left = values.get(0);
        PolicyValue right = values.get(1);
        return switch (operator) {
            case EQ -> left.equals(right);
            case NEQ -> !left.equals(right);
            case IN -> contains(right, left);
            case CONTAINS -> contains(left, right);
            case INTERSECTS -> intersects(left, right);
            case LT -> compare(left, right) < 0;
            case LTE -> compare(left, right) <= 0;
            case GT -> compare(left, right) > 0;
            case GTE -> compare(left, right) >= 0;
            case EXISTS, NOT_EXISTS -> throw new IllegalStateException("Unary operator was already evaluated");
        };
    }

    private List<PolicyValue> resolve(List<PolicyOperand> operands, PolicyAttributeSet attributes) {
        List<PolicyValue> resolved = new ArrayList<>();
        for (PolicyOperand operand : operands) {
            Optional<PolicyValue> value;
            if (operand instanceof PolicyOperand.Attribute attribute) {
                value = attributes.value(attribute.name());
            } else {
                value = Optional.of(((PolicyOperand.Literal) operand).value());
            }
            if (value.isEmpty()) {
                return List.of();
            }
            resolved.add(value.orElseThrow());
        }
        return List.copyOf(resolved);
    }

    private boolean contains(PolicyValue set, PolicyValue scalar) {
        if (set instanceof PolicyValue.LongSetValue values) {
            return values.value().contains(((PolicyValue.LongValue) scalar).value());
        }
        if (set instanceof PolicyValue.StringSetValue values) {
            return values.value().contains(((PolicyValue.StringValue) scalar).value());
        }
        if (set instanceof PolicyValue.EnumSetValue values) {
            return values.value().contains(((PolicyValue.EnumValue) scalar).value());
        }
        PolicyValue.InstantSetValue values = (PolicyValue.InstantSetValue) set;
        return values.value().contains(((PolicyValue.InstantValue) scalar).value());
    }

    private boolean intersects(PolicyValue left, PolicyValue right) {
        if (left instanceof PolicyValue.LongSetValue leftSet) {
            return leftSet.value().stream()
                    .anyMatch(((PolicyValue.LongSetValue) right).value()::contains);
        }
        if (left instanceof PolicyValue.StringSetValue leftSet) {
            return leftSet.value().stream()
                    .anyMatch(((PolicyValue.StringSetValue) right).value()::contains);
        }
        if (left instanceof PolicyValue.EnumSetValue leftSet) {
            return leftSet.value().stream()
                    .anyMatch(((PolicyValue.EnumSetValue) right).value()::contains);
        }
        PolicyValue.InstantSetValue leftSet = (PolicyValue.InstantSetValue) left;
        return leftSet.value().stream()
                .anyMatch(((PolicyValue.InstantSetValue) right).value()::contains);
    }

    private int compare(PolicyValue left, PolicyValue right) {
        if (left instanceof PolicyValue.LongValue leftValue) {
            return Long.compare(leftValue.value(), ((PolicyValue.LongValue) right).value());
        }
        PolicyValue.InstantValue leftValue = (PolicyValue.InstantValue) left;
        return leftValue.value().compareTo(((PolicyValue.InstantValue) right).value());
    }
}
