package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.Objects;

public sealed interface PolicyCondition
        permits PolicyCondition.All, PolicyCondition.Any, PolicyCondition.Predicate {

    record All(List<PolicyCondition> children) implements PolicyCondition {
        public All {
            children = List.copyOf(children);
        }
    }

    record Any(List<PolicyCondition> children) implements PolicyCondition {
        public Any {
            children = List.copyOf(children);
        }
    }

    record Predicate(PolicyOperator operator, List<PolicyOperand> operands) implements PolicyCondition {
        public Predicate {
            Objects.requireNonNull(operator);
            operands = List.copyOf(operands);
        }
    }
}
