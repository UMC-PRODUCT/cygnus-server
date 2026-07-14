package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.BOOLEAN_PREDICATE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.VALID_BUNDLE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.bytes;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.module;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyValueType;

class PolicyApprovedSchemaSemanticsTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("ATTRIBUTE-valued outcome을 compiled operand AST로 보존한다")
    @Test
    void preservesAttributeValuedOutcome() {
        String outcome = """
                [{"key":"scope.gisuIds","value":{"type":"ATTRIBUTE","name":"relation.gisuIds"}}]
                """;
        PolicyDomainSchema schema = schema(
                Set.of("subject.active", "relation.gisuIds"),
                Set.of("scope.gisuIds"),
                List.of(
                        attribute("subject.active", PolicyValueType.BOOLEAN),
                        attribute("relation.gisuIds", PolicyValueType.LONG_SET)),
                List.of(new OutcomeSchema(
                        "scope.gisuIds", PolicyValueType.LONG_SET, OutcomeMergeStrategy.SET_UNION, List.of())));

        CompiledPolicyBundle compiled = compiler.compile(request(
                bytes(VALID_BUNDLE),
                Map.of("demo.policy.json", bytes(module(BOOLEAN_PREDICATE, "[\"project:read\"]", outcome))),
                schema));

        assertThat(compiled.modules().getFirst().statements().getFirst().outcomes().getFirst().value())
                .isEqualTo(new PolicyOperand.Attribute("relation.gisuIds", PolicyValueType.LONG_SET));
    }

    @DisplayName("action schema에 허용되지 않은 attribute 참조를 거부한다")
    @Test
    void rejectsAttributeOutsideActionSchema() {
        String condition = predicate("EQ", attributeOperand("subject.name"), literal("STRING", "\"member\""));
        PolicyDomainSchema schema = schema(
                Set.of("subject.active"),
                Set.of(),
                List.of(
                        attribute("subject.active", PolicyValueType.BOOLEAN),
                        attribute("subject.name", PolicyValueType.STRING)),
                List.of());

        assertFailure(
                request(bytes(VALID_BUNDLE), Map.of("demo.policy.json", bytes(module(condition))), schema),
                PolicyFailureCode.ATTRIBUTE_NOT_ALLOWED_FOR_ACTION);
    }

    @DisplayName("action schema에 허용되지 않은 outcome을 거부한다")
    @Test
    void rejectsOutcomeOutsideActionSchema() {
        String outcomes = """
                [{"key":"force","value":{"type":"BOOLEAN","value":true}}]
                """;
        PolicyDomainSchema schema = schema(
                Set.of("subject.active"),
                Set.of(),
                List.of(attribute("subject.active", PolicyValueType.BOOLEAN)),
                List.of(new OutcomeSchema(
                        "force", PolicyValueType.BOOLEAN, OutcomeMergeStrategy.BOOLEAN_OR, List.of())));

        assertFailure(
                request(
                        bytes(VALID_BUNDLE),
                        Map.of("demo.policy.json", bytes(module(
                                BOOLEAN_PREDICATE, "[\"project:read\"]", outcomes))),
                        schema),
                PolicyFailureCode.OUTCOME_NOT_ALLOWED_FOR_ACTION);
    }

    @DisplayName("attribute enum catalog에 없는 symbol을 거부한다")
    @Test
    void rejectsUnknownEnumSymbol() {
        String condition = predicate("EQ", attributeOperand("resource.status"), literal("ENUM", "\"UNKNOWN\""));
        PolicyDomainSchema schema = schema(
                Set.of("resource.status"),
                Set.of(),
                List.of(new AttributeSchema(
                        "resource.status", PolicyValueType.ENUM, Set.of("DRAFT", "PUBLISHED"))),
                List.of());

        assertFailure(
                request(bytes(VALID_BUNDLE), Map.of("demo.policy.json", bytes(module(condition))), schema),
                PolicyFailureCode.UNKNOWN_ENUM_SYMBOL);
    }

    @DisplayName("EXISTS는 right 없이 ATTRIBUTE left만 허용한다")
    @Test
    void compilesUnaryExistsWithoutRight() {
        String condition = """
                {"type":"PREDICATE","operator":"EXISTS",
                 "left":{"type":"ATTRIBUTE","name":"subject.active"}}
                """;
        assertThatCode(() -> compiler.compile(PolicyCompilerTestFixture.request(VALID_BUNDLE, module(condition))))
                .doesNotThrowAnyException();
    }

    @DisplayName("module 간 statement ID 중복을 bundle-global로 거부한다")
    @Test
    void rejectsBundleGlobalDuplicateStatementId() {
        String bundle = """
                {"$schema":"urn:umc:authorization:policy-bundle:1.0","schemaVersion":"1.0",
                 "contextSchemaVersion":"test-1.0","namespace":"test","policyVersion":"1.0.0",
                 "defaultEffect":"DENY","combiningAlgorithm":"DENY_OVERRIDES","modules":[
                   {"id":"first","resource":"first.policy.json"},
                   {"id":"second","resource":"second.policy.json"}]}
                """;
        Map<String, byte[]> modules = Map.of(
                "first.policy.json", bytes(moduleFor("first", "same-id")),
                "second.policy.json", bytes(moduleFor("second", "same-id")));

        assertFailure(
                request(bytes(bundle), modules, PolicyCompilerTestFixture.schema()),
                PolicyFailureCode.DUPLICATE_STATEMENT);
    }

    @DisplayName("bundle과 module의 승인되지 않은 dialect 필드를 각각 거부한다")
    @Test
    void rejectsLegacyWireFields() {
        String legacyFile = VALID_BUNDLE.replace("\"resource\"", "\"file\"");
        assertFailure(
                PolicyCompilerTestFixture.request(legacyFile, module(BOOLEAN_PREDICATE)),
                PolicyFailureCode.UNKNOWN_FIELD);

        String legacyOperands = module(BOOLEAN_PREDICATE).replace(
                BOOLEAN_PREDICATE,
                """
                        {"type":"PREDICATE","operator":"EQ","operands":[
                         {"type":"ATTRIBUTE","name":"subject.active"},
                         {"type":"BOOLEAN","value":true}]}
                        """);
        assertFailure(
                PolicyCompilerTestFixture.request(VALID_BUNDLE, legacyOperands),
                PolicyFailureCode.UNKNOWN_FIELD);

        String legacyName = module(
                BOOLEAN_PREDICATE,
                "[\"project:read\"]",
                "[{\"name\":\"application.forceDecision\",\"value\":{\"type\":\"BOOLEAN\",\"value\":true}}]");
        assertFailure(
                PolicyCompilerTestFixture.request(VALID_BUNDLE, legacyName),
                PolicyFailureCode.UNKNOWN_FIELD);

        String moduleDefaults = module(BOOLEAN_PREDICATE).replace(
                "\"moduleId\":", "\"defaultEffect\":\"DENY\",\"combiningAlgorithm\":\"DENY_OVERRIDES\",\"moduleId\":");
        assertFailure(
                PolicyCompilerTestFixture.request(VALID_BUNDLE, moduleDefaults),
                PolicyFailureCode.UNKNOWN_FIELD);

        String singularSetValue = module(predicate(
                "IN",
                attributeOperand("subject.name"),
                "{\"type\":\"STRING_SET\",\"value\":[\"member\"]}"));
        assertFailure(
                PolicyCompilerTestFixture.request(VALID_BUNDLE, singularSetValue),
                PolicyFailureCode.UNKNOWN_FIELD);
    }

    @DisplayName("bundle과 module의 $schema URN을 exact match로 검증한다")
    @Test
    void rejectsMismatchedSchemaUrn() {
        assertFailure(
                PolicyCompilerTestFixture.request(
                        VALID_BUNDLE.replace(
                                "urn:umc:authorization:policy-bundle:1.0",
                                "urn:umc:authorization:policy-bundle:1.1"),
                        module(BOOLEAN_PREDICATE)),
                PolicyFailureCode.ENVELOPE_MISMATCH);
        assertFailure(
                PolicyCompilerTestFixture.request(
                        VALID_BUNDLE,
                        module(BOOLEAN_PREDICATE).replace(
                                "urn:umc:authorization:policy-module:1.0",
                                "urn:umc:authorization:policy-module:1.1")),
                PolicyFailureCode.ENVELOPE_MISMATCH);
    }

    private PolicyDomainSchema schema(
            Set<String> allowedAttributes,
            Set<String> allowedOutcomes,
            List<AttributeSchema> attributes,
            List<OutcomeSchema> outcomes) {
        return new PolicyDomainSchema(
                "test-1.0",
                List.of(new ActionSchema(
                        "project:read", allowedAttributes, Set.of(), allowedOutcomes)),
                attributes,
                outcomes);
    }

    private AttributeSchema attribute(String name, PolicyValueType type) {
        return new AttributeSchema(name, type, Set.of());
    }

    private String predicate(String operator, String left, String right) {
        return "{\"type\":\"PREDICATE\",\"operator\":\""
                + operator
                + "\",\"left\":"
                + left
                + ",\"right\":"
                + right
                + "}";
    }

    private String attributeOperand(String name) {
        return "{\"type\":\"ATTRIBUTE\",\"name\":\"" + name + "\"}";
    }

    private String literal(String type, String value) {
        return "{\"type\":\"" + type + "\",\"value\":" + value + "}";
    }

    private String moduleFor(String moduleId, String statementId) {
        return module(BOOLEAN_PREDICATE)
                .replace("\"moduleId\": \"demo\"", "\"moduleId\": \"" + moduleId + "\"")
                .replace("\"id\": \"allow-active\"", "\"id\": \"" + statementId + "\"");
    }

    private void assertFailure(PolicyBundleCompilationRequest request, PolicyFailureCode code) {
        PolicyCompilationException exception = failure(compiler, request);
        assertThat(exception.code()).isEqualTo(code);
    }
}
