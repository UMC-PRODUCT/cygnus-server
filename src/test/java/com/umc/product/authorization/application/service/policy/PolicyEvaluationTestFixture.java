package com.umc.product.authorization.application.service.policy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

final class PolicyEvaluationTestFixture {

    static final String ACTION = "test:evaluate";
    static final Instant EVALUATED_AT = Instant.parse("2026-07-13T03:00:00Z");

    static final PolicyAttributeKey<PolicyValue.BooleanValue> REQUIRED_ACTIVE =
            new PolicyAttributeKey<>("required.active", PolicyValueType.BOOLEAN);
    static final PolicyAttributeKey<PolicyValue.BooleanValue> OPTIONAL_FLAG =
            new PolicyAttributeKey<>("optional.flag", PolicyValueType.BOOLEAN);
    static final PolicyAttributeKey<PolicyValue.LongValue> OPTIONAL_LONG =
            new PolicyAttributeKey<>("optional.long", PolicyValueType.LONG);
    static final PolicyAttributeKey<PolicyValue.LongValue> OPTIONAL_OTHER_LONG =
            new PolicyAttributeKey<>("optional.otherLong", PolicyValueType.LONG);
    static final PolicyAttributeKey<PolicyValue.StringValue> OPTIONAL_NAME =
            new PolicyAttributeKey<>("optional.name", PolicyValueType.STRING);
    static final PolicyAttributeKey<PolicyValue.EnumValue> OPTIONAL_STATUS =
            new PolicyAttributeKey<>("optional.status", PolicyValueType.ENUM);
    static final PolicyAttributeKey<PolicyValue.InstantValue> OPTIONAL_INSTANT =
            new PolicyAttributeKey<>("optional.instant", PolicyValueType.INSTANT);
    static final PolicyAttributeKey<PolicyValue.LongSetValue> OPTIONAL_LONGS =
            new PolicyAttributeKey<>("optional.longs", PolicyValueType.LONG_SET);
    static final PolicyAttributeKey<PolicyValue.StringSetValue> OPTIONAL_NAMES =
            new PolicyAttributeKey<>("optional.names", PolicyValueType.STRING_SET);
    static final PolicyAttributeKey<PolicyValue.EnumSetValue> OPTIONAL_STATUSES =
            new PolicyAttributeKey<>("optional.statuses", PolicyValueType.ENUM_SET);
    static final PolicyAttributeKey<PolicyValue.InstantSetValue> OPTIONAL_INSTANTS =
            new PolicyAttributeKey<>("optional.instants", PolicyValueType.INSTANT_SET);

    private PolicyEvaluationTestFixture() {}

    static CompiledPolicyBundle compile(String... statements) {
        String module = """
                {
                  "$schema":"urn:umc:authorization:policy-module:1.0",
                  "schemaVersion":"1.0",
                  "contextSchemaVersion":"test-1.0",
                  "namespace":"test",
                  "policyVersion":"1.0.0",
                  "moduleId":"demo",
                  "statements":[%s]
                }
                """
                .formatted(String.join(",", statements));
        PolicyBundleCompilationRequest request = new PolicyBundleCompilationRequest(
                bytes(bundle()), Map.of("demo.policy.json", bytes(module)), schema());
        return new PolicySemanticCompiler().compile(request);
    }

    static PolicyEvaluationRequest request(CompiledPolicyBundle bundle, PolicyAttributeSet attributes) {
        return new PolicyEvaluationRequest(bundle, ACTION, attributes, EVALUATED_AT);
    }

    static PolicyAttributeSet requiredActive(boolean active) {
        return PolicyAttributeSet.builder()
                .put(REQUIRED_ACTIVE, new PolicyValue.BooleanValue(active))
                .build();
    }

    static String statement(String id, String effect, String condition) {
        return statement(id, effect, condition, "[]");
    }

    static String statement(String id, String effect, String condition, String outcomes) {
        return statementWithActions(id, effect, "[\"" + ACTION + "\"]", condition, outcomes);
    }

    static String statementWithActions(
            String id, String effect, String actions, String condition, String outcomes) {
        return """
                {
                  "id":"%s",
                  "actions":%s,
                  "effect":"%s",
                  "condition":%s,
                  "outcomes":%s
                }
                """
                .formatted(id, actions, effect, condition, outcomes);
    }

    static String eqActive(boolean value) {
        return predicate(
                "EQ",
                attribute("required.active"),
                literal("BOOLEAN", Boolean.toString(value)));
    }

    static String predicate(String operator, String left) {
        return """
                {"type":"PREDICATE","operator":"%s","left":%s}
                """
                .formatted(operator, left);
    }

    static String predicate(String operator, String left, String right) {
        return """
                {"type":"PREDICATE","operator":"%s","left":%s,"right":%s}
                """
                .formatted(operator, left, right);
    }

    static String attribute(String name) {
        return """
                {"type":"ATTRIBUTE","name":"%s"}
                """
                .formatted(name);
    }

    static String literal(String type, String value) {
        String field = type.endsWith("_SET") ? "values" : "value";
        return """
                {"type":"%s","%s":%s}
                """
                .formatted(type, field, value);
    }

    static String outcome(String key, String operand) {
        return """
                {"key":"%s","value":%s}
                """
                .formatted(key, operand);
    }

    static PolicyDomainSchema schema() {
        Set<String> attributes = Set.of(
                "required.active",
                "optional.flag",
                "optional.long",
                "optional.otherLong",
                "optional.name",
                "optional.status",
                "optional.instant",
                "optional.longs",
                "optional.names",
                "optional.statuses",
                "optional.instants");
        Set<String> outcomes = Set.of(
                "outcome.boolean",
                "outcome.longs",
                "outcome.view",
                "outcome.exactly",
                "outcome.attributeLongs",
                "outcome.strings",
                "outcome.enums",
                "outcome.instants");
        return new PolicyDomainSchema(
                "test-1.0",
                List.of(
                        new ActionSchema(
                                ACTION, Set.of("required.active"), withoutRequired(attributes), outcomes),
                        new ActionSchema(
                                "test:other",
                                Set.of("required.active"),
                                withoutRequired(attributes),
                                outcomes)),
                List.of(
                        attributeSchema("required.active", PolicyValueType.BOOLEAN),
                        attributeSchema("optional.flag", PolicyValueType.BOOLEAN),
                        attributeSchema("optional.long", PolicyValueType.LONG),
                        attributeSchema("optional.otherLong", PolicyValueType.LONG),
                        attributeSchema("optional.name", PolicyValueType.STRING),
                        new AttributeSchema("optional.status", PolicyValueType.ENUM, Set.of("LOW", "HIGH")),
                        attributeSchema("optional.instant", PolicyValueType.INSTANT),
                        attributeSchema("optional.longs", PolicyValueType.LONG_SET),
                        attributeSchema("optional.names", PolicyValueType.STRING_SET),
                        new AttributeSchema(
                                "optional.statuses", PolicyValueType.ENUM_SET, Set.of("LOW", "HIGH")),
                        attributeSchema("optional.instants", PolicyValueType.INSTANT_SET)),
                List.of(
                        new OutcomeSchema(
                                "outcome.boolean",
                                PolicyValueType.BOOLEAN,
                                OutcomeMergeStrategy.BOOLEAN_OR,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.longs",
                                PolicyValueType.LONG_SET,
                                OutcomeMergeStrategy.SET_UNION,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.view",
                                PolicyValueType.ENUM,
                                OutcomeMergeStrategy.DOMINANCE,
                                List.of("FULL", "APPLICANT", "NONE")),
                        new OutcomeSchema(
                                "outcome.exactly",
                                PolicyValueType.STRING,
                                OutcomeMergeStrategy.EXACTLY_ONE,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.attributeLongs",
                                PolicyValueType.LONG_SET,
                                OutcomeMergeStrategy.SET_UNION,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.strings",
                                PolicyValueType.STRING_SET,
                                OutcomeMergeStrategy.SET_UNION,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.enums",
                                PolicyValueType.ENUM_SET,
                                OutcomeMergeStrategy.SET_UNION,
                                List.of()),
                        new OutcomeSchema(
                                "outcome.instants",
                                PolicyValueType.INSTANT_SET,
                                OutcomeMergeStrategy.SET_UNION,
                                List.of())));
    }

    private static Set<String> withoutRequired(Set<String> attributes) {
        java.util.Set<String> optional = new java.util.HashSet<>(attributes);
        optional.remove("required.active");
        return Set.copyOf(optional);
    }

    private static AttributeSchema attributeSchema(String name, PolicyValueType type) {
        return new AttributeSchema(name, type, Set.of());
    }

    private static String bundle() {
        return """
                {
                  "$schema":"urn:umc:authorization:policy-bundle:1.0",
                  "schemaVersion":"1.0",
                  "contextSchemaVersion":"test-1.0",
                  "namespace":"test",
                  "policyVersion":"1.0.0",
                  "defaultEffect":"DENY",
                  "combiningAlgorithm":"DENY_OVERRIDES",
                  "modules":[{"id":"demo","resource":"demo.policy.json"}]
                }
                """;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
