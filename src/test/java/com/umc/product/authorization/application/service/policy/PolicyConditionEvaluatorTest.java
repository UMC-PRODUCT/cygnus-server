package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_FLAG;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_INSTANT;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_INSTANTS;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_LONG;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_LONGS;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_NAME;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_NAMES;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_STATUS;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.OPTIONAL_STATUSES;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.REQUIRED_ACTIVE;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.attribute;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.compile;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.literal;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.predicate;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.requiredActive;
import static com.umc.product.authorization.application.service.policy.PolicyEvaluationTestFixture.statement;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;

class PolicyConditionEvaluatorTest {

    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @DisplayName("각 operator는 정확한 typed operand를 평가한다")
    @MethodSource("matchingOperators")
    @ParameterizedTest(name = "{0}")
    void evaluatesTypedOperator(String name, String condition, PolicyAttributeSet attributes) {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", condition));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, attributes));

        // then
        assertThat(decision.effect()).as(name).isEqualTo(PolicyEffect.ALLOW);
    }

    @DisplayName("Optional attribute가 없으면 NOT_EXISTS만 true이고 NEQ를 포함한 나머지는 false다")
    @MethodSource("missingOptionalOperators")
    @ParameterizedTest(name = "{0}")
    void appliesMissingOptionalSemantics(String name, String condition, PolicyEffect expected) {
        // given
        CompiledPolicyBundle bundle = compile(statement("allow", "ALLOW", condition));

        // when
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(bundle, requiredActive(true)));

        // then
        assertThat(decision.effect()).as(name).isEqualTo(expected);
    }

    @DisplayName("LONG과 INSTANT 비교는 같은 ordering 의미를 사용한다")
    @Test
    void evaluatesInstantOrdering() {
        // given
        String condition = predicate(
                "GTE", attribute("optional.instant"), literal("INSTANT", "\"2026-07-13T02:00:00Z\""));
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true))
                .put(OPTIONAL_INSTANT, new PolicyValue.InstantValue(Instant.parse("2026-07-13T03:00:00Z")))
                .build();

        // when
        PolicyDecision decision =
                (PolicyDecision) evaluator.evaluate(request(compile(statement("allow", "ALLOW", condition)), attributes));

        // then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    private static Stream<Arguments> matchingOperators() {
        Instant instant = Instant.parse("2026-07-13T03:00:00Z");
        return Stream.of(
                Arguments.of(
                        "EQ",
                        predicate("EQ", attribute("optional.flag"), literal("BOOLEAN", "true")),
                        attributes(builder -> builder.put(OPTIONAL_FLAG, new PolicyValue.BooleanValue(true)))),
                Arguments.of(
                        "NEQ",
                        predicate("NEQ", attribute("optional.long"), literal("LONG", "1")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(2L)))),
                Arguments.of(
                        "IN-LONG",
                        predicate("IN", attribute("optional.long"), literal("LONG_SET", "[1,2]")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(2L)))),
                Arguments.of(
                        "IN-STRING",
                        predicate("IN", attribute("optional.name"), literal("STRING_SET", "[\"member\"]")),
                        attributes(builder -> builder.put(OPTIONAL_NAME, new PolicyValue.StringValue("member")))),
                Arguments.of(
                        "IN-ENUM",
                        predicate("IN", attribute("optional.status"), literal("ENUM_SET", "[\"HIGH\"]")),
                        attributes(builder -> builder.put(OPTIONAL_STATUS, new PolicyValue.EnumValue("HIGH")))),
                Arguments.of(
                        "IN-INSTANT",
                        predicate(
                                "IN",
                                attribute("optional.instant"),
                                literal("INSTANT_SET", "[\"2026-07-13T03:00:00Z\"]")),
                        attributes(builder -> builder.put(OPTIONAL_INSTANT, new PolicyValue.InstantValue(instant)))),
                Arguments.of(
                        "CONTAINS",
                        predicate(
                                "CONTAINS", attribute("optional.names"), literal("STRING", "\"member\"")),
                        attributes(builder -> builder.put(
                                OPTIONAL_NAMES, new PolicyValue.StringSetValue(Set.of("member"))))),
                Arguments.of(
                        "INTERSECTS-LONG",
                        predicate("INTERSECTS", attribute("optional.longs"), literal("LONG_SET", "[2,3]")),
                        attributes(builder -> builder.put(
                                OPTIONAL_LONGS, new PolicyValue.LongSetValue(Set.of(1L, 2L))))),
                Arguments.of(
                        "INTERSECTS-STRING",
                        predicate(
                                "INTERSECTS",
                                attribute("optional.names"),
                                literal("STRING_SET", "[\"member\",\"other\"]")),
                        attributes(builder -> builder.put(
                                OPTIONAL_NAMES, new PolicyValue.StringSetValue(Set.of("member"))))),
                Arguments.of(
                        "INTERSECTS-ENUM",
                        predicate(
                                "INTERSECTS",
                                attribute("optional.statuses"),
                                literal("ENUM_SET", "[\"HIGH\"]")),
                        attributes(builder -> builder.put(
                                OPTIONAL_STATUSES, new PolicyValue.EnumSetValue(Set.of("LOW", "HIGH"))))),
                Arguments.of(
                        "INTERSECTS-INSTANT",
                        predicate(
                                "INTERSECTS",
                                attribute("optional.instants"),
                                literal("INSTANT_SET", "[\"2026-07-13T03:00:00Z\"]")),
                        attributes(builder -> builder.put(
                                OPTIONAL_INSTANTS, new PolicyValue.InstantSetValue(Set.of(instant))))),
                Arguments.of(
                        "EXISTS",
                        predicate("EXISTS", attribute("optional.name")),
                        attributes(builder -> builder.put(OPTIONAL_NAME, new PolicyValue.StringValue("member")))),
                Arguments.of(
                        "NOT_EXISTS",
                        predicate("NOT_EXISTS", attribute("optional.name")),
                        requiredActive(true)),
                Arguments.of(
                        "LT",
                        predicate("LT", attribute("optional.long"), literal("LONG", "2")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(1L)))),
                Arguments.of(
                        "LTE",
                        predicate("LTE", attribute("optional.long"), literal("LONG", "1")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(1L)))),
                Arguments.of(
                        "GT",
                        predicate("GT", attribute("optional.long"), literal("LONG", "1")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(2L)))),
                Arguments.of(
                        "GTE",
                        predicate("GTE", attribute("optional.long"), literal("LONG", "2")),
                        attributes(builder -> builder.put(OPTIONAL_LONG, new PolicyValue.LongValue(2L)))));
    }

    private static Stream<Arguments> missingOptionalOperators() {
        return Stream.of(
                Arguments.of(
                        "EQ", predicate("EQ", attribute("optional.name"), literal("STRING", "\"x\"")), PolicyEffect.DENY),
                Arguments.of(
                        "NEQ",
                        predicate("NEQ", attribute("optional.name"), literal("STRING", "\"x\"")),
                        PolicyEffect.DENY),
                Arguments.of(
                        "IN",
                        predicate("IN", attribute("optional.name"), literal("STRING_SET", "[\"x\"]")),
                        PolicyEffect.DENY),
                Arguments.of(
                        "CONTAINS",
                        predicate("CONTAINS", attribute("optional.names"), literal("STRING", "\"x\"")),
                        PolicyEffect.DENY),
                Arguments.of(
                        "INTERSECTS",
                        predicate(
                                "INTERSECTS", attribute("optional.names"), literal("STRING_SET", "[\"x\"]")),
                        PolicyEffect.DENY),
                Arguments.of("EXISTS", predicate("EXISTS", attribute("optional.name")), PolicyEffect.DENY),
                Arguments.of("NOT_EXISTS", predicate("NOT_EXISTS", attribute("optional.name")), PolicyEffect.ALLOW),
                Arguments.of(
                        "LT", predicate("LT", attribute("optional.long"), literal("LONG", "1")), PolicyEffect.DENY),
                Arguments.of(
                        "LTE", predicate("LTE", attribute("optional.long"), literal("LONG", "1")), PolicyEffect.DENY),
                Arguments.of(
                        "GT", predicate("GT", attribute("optional.long"), literal("LONG", "1")), PolicyEffect.DENY),
                Arguments.of(
                        "GTE", predicate("GTE", attribute("optional.long"), literal("LONG", "1")), PolicyEffect.DENY));
    }

    private static PolicyAttributeSet attributes(BuilderCustomizer customizer) {
        PolicyAttributeSet.Builder builder =
                PolicyAttributeSet.builder().put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(true));
        customizer.customize(builder);
        return builder.build();
    }

    @FunctionalInterface
    private interface BuilderCustomizer {
        void customize(PolicyAttributeSet.Builder builder);
    }
}
