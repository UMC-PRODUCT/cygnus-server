package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_FLAG;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.REQUIRED_ACTIVE;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.eqActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

class PolicyEngineValueObjectTest {

    @DisplayName("PolicyValue set은 source 변경과 무관하고 canonical order를 유지한다")
    @Test
    void policySetValueIsImmutableAndCanonical() {
        // given
        Set<Long> source = new HashSet<>(Set.of(3L, 1L));

        // when
        PolicyValue.LongSetValue value = new PolicyValue.LongSetValue(source);
        source.add(2L);

        // then
        assertThat(new ArrayList<>(value.value())).containsExactly(1L, 3L);
        assertThatThrownBy(() -> value.value().add(4L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @DisplayName("PolicyAttributeSet은 build 시점 snapshot이며 key 중복을 거부한다")
    @Test
    void policyAttributeSetIsImmutableAndRejectsDuplicates() {
        // given
        PolicyAttributeSet.Builder builder =
                PolicyAttributeSet.builder().put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true));

        // when
        PolicyAttributeSet snapshot = builder.build();
        builder.put(OPTIONAL_FLAG, new PolicyValue.BooleanValue(false));

        // then
        assertThat(snapshot.entries()).extracting(attribute -> attribute.name()).containsExactly("required.active");
        assertThatThrownBy(() -> builder.put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("PolicyAttributeSet builder는 key와 value의 typed contract 불일치를 거부한다")
    @Test
    void rejectsAttributeKeyValueTypeMismatch() {
        // given
        PolicyAttributeKey<PolicyValue.BooleanValue> wrong =
                new PolicyAttributeKey<>("required.active", PolicyValueType.LONG);

        // when & then
        assertThatThrownBy(() -> PolicyAttributeSet.builder().put(wrong, new PolicyValue.BooleanValue(true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("CompiledPolicyBundle은 module collection과 domain schema를 immutable하게 보존한다")
    @Test
    void compiledBundleIsImmutable() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));

        // when & then
        assertThatThrownBy(() -> bundle.modules().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(bundle.domainSchema().action("test:evaluate")).isPresent();
    }

    @DisplayName("DOMINANCE order의 중복 symbol은 거부한다")
    @Test
    void rejectsDuplicateDominanceOrder() {
        // when & then
        assertThatThrownBy(() -> new OutcomeSchema(
                        "outcome.view",
                        PolicyValueType.ENUM,
                        OutcomeMergeStrategy.DOMINANCE,
                        List.of("FULL", "FULL")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
