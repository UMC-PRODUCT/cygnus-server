package com.umc.product.authorization.domain.policy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompiledPolicyBundleActionIndexTest {

    @Test
    @DisplayName("외부에서 modules와 다른 action index를 주입할 수 없다")
    void rejectsInjectedActionIndex() {
        CompiledPolicyStatement statement = new CompiledPolicyStatement(
            "allow",
            List.of("demo:read"),
            PolicyEffect.ALLOW,
            new PolicyCondition.Predicate(
                PolicyOperator.EQ,
                List.of(
                    new PolicyOperand.Attribute("subject.active", PolicyValueType.BOOLEAN),
                    new PolicyOperand.Literal(new PolicyValue.BooleanValue(true)))),
            List.of());
        CompiledPolicyModule module = new CompiledPolicyModule(
            "demo",
            "demo.policy.json",
            List.of(statement));
        PolicyDomainSchema schema = new PolicyDomainSchema(
            "demo-1.0",
            List.of(new ActionSchema(
                "demo:read",
                Set.of("subject.active"),
                Set.of(),
                Set.of())),
            List.of(new AttributeSchema("subject.active", PolicyValueType.BOOLEAN, Set.of())),
            List.of());

        assertThatThrownBy(() -> new CompiledPolicyBundle(
            "1.0",
            "demo-1.0",
            "demo",
            "1.0.0",
            PolicyEffect.DENY,
            PolicyCombiningAlgorithm.DENY_OVERRIDES,
            schema,
            "0".repeat(64),
            List.of(module),
            Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("action index");
    }
}
