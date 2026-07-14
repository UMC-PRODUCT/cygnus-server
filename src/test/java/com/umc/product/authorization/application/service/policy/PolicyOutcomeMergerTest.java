package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_LONGS;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_STATUS;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.REQUIRED_ACTIVE;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.attribute;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.eqActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.literal;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.outcome;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.requiredActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyValue;

class PolicyOutcomeMergerTest {

    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @DisplayName("BOOLEAN_OR outcome은 매칭된 값 중 true를 보존한다")
    @Test
    void mergesBooleanOr() {
        // given
        String falseOutcome = "[" + outcome("outcome.boolean", literal("BOOLEAN", "false")) + "]";
        String trueOutcome = "[" + outcome("outcome.boolean", literal("BOOLEAN", "true")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("false-value", "ALLOW", eqActive(true), falseOutcome),
                statement("true-value", "ALLOW", eqActive(true), trueOutcome));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.outcome("outcome.boolean")).contains(new PolicyValue.BooleanValue(true));
    }

    @DisplayName("SET_UNION outcome은 중복을 제거하고 값을 canonical order로 반환한다")
    @Test
    void mergesSetUnionInCanonicalOrder() {
        // given
        String first = "[" + outcome("outcome.longs", literal("LONG_SET", "[3,1]")) + "]";
        String second = "[" + outcome("outcome.longs", literal("LONG_SET", "[2,1]")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("first", "ALLOW", eqActive(true), first),
                statement("second", "ALLOW", eqActive(true), second));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        PolicyValue.LongSetValue value = (PolicyValue.LongSetValue)
                decision.outcome("outcome.longs").orElseThrow();
        assertThat(new ArrayList<>(value.value())).containsExactly(1L, 2L, 3L);
    }

    @DisplayName("SET_UNION은 STRING, ENUM, INSTANT set도 타입별 canonical order로 합친다")
    @Test
    void mergesEverySetValueType() {
        // given
        String first = "["
                + outcome("outcome.strings", literal("STRING_SET", "[\"b\",\"a\"]"))
                + ","
                + outcome("outcome.enums", literal("ENUM_SET", "[\"LOW\"]"))
                + ","
                + outcome(
                        "outcome.instants",
                        literal("INSTANT_SET", "[\"2026-07-13T03:00:00Z\"]"))
                + "]";
        String second = "["
                + outcome("outcome.strings", literal("STRING_SET", "[\"c\"]"))
                + ","
                + outcome("outcome.enums", literal("ENUM_SET", "[\"HIGH\"]"))
                + ","
                + outcome(
                        "outcome.instants",
                        literal("INSTANT_SET", "[\"2026-07-13T01:00:00Z\"]"))
                + "]";
        CompiledPolicyBundle bundle = compile(
                statement("first", "ALLOW", eqActive(true), first),
                statement("second", "ALLOW", eqActive(true), second));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        PolicyValue.StringSetValue strings =
                (PolicyValue.StringSetValue) decision.outcome("outcome.strings").orElseThrow();
        PolicyValue.EnumSetValue enums =
                (PolicyValue.EnumSetValue) decision.outcome("outcome.enums").orElseThrow();
        PolicyValue.InstantSetValue instants =
                (PolicyValue.InstantSetValue) decision.outcome("outcome.instants").orElseThrow();
        assertThat(new ArrayList<>(strings.value())).containsExactly("a", "b", "c");
        assertThat(new ArrayList<>(enums.value())).containsExactly("HIGH", "LOW");
        assertThat(new ArrayList<>(instants.value()))
                .containsExactly(
                        java.time.Instant.parse("2026-07-13T01:00:00Z"),
                        java.time.Instant.parse("2026-07-13T03:00:00Z"));
    }

    @DisplayName("DOMINANCE outcome은 선언 순서에서 가장 높은 값을 선택한다")
    @Test
    void mergesByDominance() {
        // given
        String applicant = "[" + outcome("outcome.view", literal("ENUM", "\"APPLICANT\"")) + "]";
        String full = "[" + outcome("outcome.view", literal("ENUM", "\"FULL\"")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("applicant", "ALLOW", eqActive(true), applicant),
                statement("full", "ALLOW", eqActive(true), full));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.outcome("outcome.view")).contains(new PolicyValue.EnumValue("FULL"));
    }

    @DisplayName("EXACTLY_ONE outcome은 동일 값이 반복되면 하나로 합친다")
    @Test
    void acceptsRepeatedExactlyOneValue() {
        // given
        String same = "[" + outcome("outcome.exactly", literal("STRING", "\"same\"")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("first", "ALLOW", eqActive(true), same),
                statement("second", "ALLOW", eqActive(true), same));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.outcome("outcome.exactly")).contains(new PolicyValue.StringValue("same"));
    }

    @DisplayName("EXACTLY_ONE outcome에 서로 다른 값이 매칭되면 evaluation failure다")
    @Test
    void failsForDifferentExactlyOneValues() {
        // given
        String first = "[" + outcome("outcome.exactly", literal("STRING", "\"first\"")) + "]";
        String second = "[" + outcome("outcome.exactly", literal("STRING", "\"second\"")) + "]";
        CompiledPolicyBundle bundle = compile(
                statement("alpha", "ALLOW", eqActive(true), first),
                statement("beta", "ALLOW", eqActive(true), second));

        // when
        PolicyEvaluationFailure failure =
                (PolicyEvaluationFailure) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.EXACTLY_ONE_CONFLICT);
        assertThat(failure.statementId()).contains("beta");
        assertThat(failure.outcomeKey()).contains("outcome.exactly");
    }

    @DisplayName("ATTRIBUTE outcome은 runtime context의 typed 값을 해석한다")
    @Test
    void resolvesAttributeOutcome() {
        // given
        String attributeOutcome =
                "[" + outcome("outcome.attributeLongs", attribute("optional.longs")) + "]";
        CompiledPolicyBundle bundle =
                compile(statement("allow", "ALLOW", eqActive(true), attributeOutcome));
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true))
                .put(OPTIONAL_LONGS, new PolicyValue.LongSetValue(Set.of(3L, 1L)))
                .build();

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, attributes));

        // then
        PolicyValue.LongSetValue value = (PolicyValue.LongSetValue)
                decision.outcome("outcome.attributeLongs").orElseThrow();
        assertThat(new ArrayList<>(value.value())).containsExactly(1L, 3L);
    }

    @DisplayName("매칭된 ATTRIBUTE outcome 값이 context에 없으면 evaluation failure다")
    @Test
    void failsForMissingAttributeOutcome() {
        // given
        String attributeOutcome =
                "[" + outcome("outcome.attributeLongs", attribute("optional.longs")) + "]";
        CompiledPolicyBundle bundle =
                compile(statement("allow", "ALLOW", eqActive(true), attributeOutcome));

        // when
        PolicyEvaluationFailure failure =
                (PolicyEvaluationFailure) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.OUTCOME_ATTRIBUTE_MISSING);
        assertThat(failure.statementId()).contains("allow");
        assertThat(failure.attributeName()).contains("optional.longs");
        assertThat(failure.outcomeKey()).contains("outcome.attributeLongs");
    }

    @DisplayName("runtime enum symbol이 attribute catalog에 없으면 evaluation failure다")
    @Test
    void failsForUnknownRuntimeEnumSymbol() {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", eqActive(true)));
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true))
                .put(OPTIONAL_STATUS, new PolicyValue.EnumValue("UNKNOWN"))
                .build();

        // when
        PolicyEvaluationFailure failure =
                (PolicyEvaluationFailure) evaluator.evaluate(request(bundle, attributes));

        // then
        assertThat(failure.failureCode()).isEqualTo(PolicyEvaluationFailureCode.ATTRIBUTE_ENUM_SYMBOL_UNKNOWN);
        assertThat(failure.attributeName()).contains("optional.status");
    }
}
