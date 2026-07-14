package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.BOOLEAN_PREDICATE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.VALID_BUNDLE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.bytes;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.module;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;
import com.umc.product.authorization.domain.policy.PolicyValueType;

class PolicyStructuralLimitsTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("bundle 총 크기 1MiB는 허용하고 이를 1 byte 넘으면 거부한다")
    @Test
    void enforcesExactBundleSizeLimit() {
        // given
        byte[] module = bytes(module(BOOLEAN_PREDICATE));
        byte[] originalBundle = bytes(VALID_BUNDLE);
        int paddedLength = PolicyJsonParser.MAX_BUNDLE_BYTES - module.length;
        byte[] exactBundle = Arrays.copyOf(originalBundle, paddedLength);
        Arrays.fill(exactBundle, originalBundle.length, exactBundle.length, (byte) ' ');

        // when & then
        assertThatCode(() -> compiler.compile(request(
                        exactBundle, Map.of("demo.policy.json", module), schema())))
                .doesNotThrowAnyException();

        byte[] oversizedBundle = Arrays.copyOf(exactBundle, exactBundle.length + 1);
        oversizedBundle[oversizedBundle.length - 1] = ' ';
        assertFailure(
                request(oversizedBundle, Map.of("demo.policy.json", module), schema()),
                PolicyFailureCode.DOCUMENT_TOO_LARGE);
    }

    @DisplayName("statement 500개는 허용하고 501개는 거부한다")
    @Test
    void enforcesStatementLimit() {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, moduleWithStatements(500))))
                .doesNotThrowAnyException();
        assertFailure(
                request(VALID_BUNDLE, moduleWithStatements(501)),
                PolicyFailureCode.STATEMENT_LIMIT_EXCEEDED);
    }

    @DisplayName("statement action 32개는 허용하고 33개는 거부한다")
    @Test
    void enforcesActionLimit() {
        PolicyDomainSchema exactSchema = schemaWithActions(32);
        assertThatCode(() -> compiler.compile(PolicyCompilerTestFixture.request(
                        bytes(VALID_BUNDLE),
                        Map.of("demo.policy.json", bytes(module(BOOLEAN_PREDICATE, actions(32), "[]"))),
                        exactSchema)))
                .doesNotThrowAnyException();

        assertFailure(
                PolicyCompilerTestFixture.request(
                        bytes(VALID_BUNDLE),
                        Map.of("demo.policy.json", bytes(module(BOOLEAN_PREDICATE, actions(33), "[]"))),
                        schemaWithActions(33)),
                PolicyFailureCode.ACTION_LIMIT_EXCEEDED);
    }

    @DisplayName("condition depth 16은 허용하고 17은 거부한다")
    @Test
    void enforcesConditionDepthLimit() {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(nestedCondition(15)))))
                .doesNotThrowAnyException();
        assertFailure(
                request(VALID_BUNDLE, module(nestedCondition(16))),
                PolicyFailureCode.CONDITION_DEPTH_LIMIT_EXCEEDED);
    }

    @DisplayName("condition 자식 64개는 허용하고 65개는 거부한다")
    @Test
    void enforcesConditionChildLimit() {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(wideCondition(64)))))
                .doesNotThrowAnyException();
        assertFailure(
                request(VALID_BUNDLE, module(wideCondition(65))),
                PolicyFailureCode.CONDITION_CHILD_LIMIT_EXCEEDED);
    }

    @DisplayName("statement condition node가 128개를 넘으면 거부한다")
    @Test
    void enforcesConditionNodeLimit() {
        String childGroup = "{\"type\":\"ALL\",\"conditions\":[" + BOOLEAN_PREDICATE + "]}";
        String exactCondition = "{\"type\":\"ALL\",\"conditions\":["
                + String.join(",", java.util.Collections.nCopies(63, childGroup))
                + ","
                + BOOLEAN_PREDICATE
                + "]}";
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(exactCondition))))
                .doesNotThrowAnyException();

        String oversizedCondition = "{\"type\":\"ALL\",\"conditions\":["
                + String.join(",", java.util.Collections.nCopies(64, childGroup))
                + "]}";

        assertFailure(
                request(VALID_BUNDLE, module(oversizedCondition)),
                PolicyFailureCode.CONDITION_NODE_LIMIT_EXCEEDED);
    }

    @DisplayName("set 원소 256개는 허용하고 257개는 거부한다")
    @Test
    void enforcesSetLimit() {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(setCondition(256)))))
                .doesNotThrowAnyException();
        assertFailure(
                request(VALID_BUNDLE, module(setCondition(257))),
                PolicyFailureCode.SET_LIMIT_EXCEEDED);
    }

    @DisplayName("identifier 128자는 허용하고 129자는 거부한다")
    @Test
    void enforcesIdentifierLimit() {
        String exact = "a".repeat(128);
        assertThatCode(() -> compiler.compile(requestWithSingleAction(exact)))
                .doesNotThrowAnyException();

        String oversized = "a".repeat(129);
        assertFailure(requestWithSingleAction(oversized), PolicyFailureCode.IDENTIFIER_LIMIT_EXCEEDED);
    }

    @DisplayName("string 1024자는 허용하고 1025자는 거부한다")
    @Test
    void enforcesStringLimit() {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(stringCondition("a".repeat(1024))))))
                .doesNotThrowAnyException();
        assertFailure(
                request(VALID_BUNDLE, module(stringCondition("a".repeat(1025)))),
                PolicyFailureCode.STRING_LIMIT_EXCEEDED);
    }

    private void assertFailure(
            com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest request,
            PolicyFailureCode expected) {
        PolicyCompilationException exception = failure(compiler, request);
        assertThat(exception.code()).isEqualTo(expected);
    }

    private String moduleWithStatements(int count) {
        String statements = IntStream.range(0, count)
                .mapToObj(index -> """
                        {
                          "id":"statement-%d",
                          "actions":["project:read"],
                          "effect":"ALLOW",
                          "condition":%s,
                          "outcomes":[]
                        }
                        """.formatted(index, BOOLEAN_PREDICATE))
                .collect(Collectors.joining(","));
        return """
                {
                  "$schema":"urn:umc:authorization:policy-module:1.0",
                  "schemaVersion":"1.0",
                  "contextSchemaVersion":"test-1.0",
                  "namespace":"test",
                  "policyVersion":"1.0.0",
                  "moduleId":"demo",
                  "statements":[%s]
                }
                """.formatted(statements);
    }

    private String actions(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "\"action:" + index + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private PolicyDomainSchema schemaWithActions(int count) {
        List<ActionSchema> actionSchemas = IntStream.range(0, count)
                .mapToObj(index -> new ActionSchema(
                        "action:" + index, Set.of("subject.active"), Set.of(), Set.of()))
                .toList();
        return new PolicyDomainSchema(
                "test-1.0",
                actionSchemas,
                List.of(new AttributeSchema("subject.active", PolicyValueType.BOOLEAN, Set.of())),
                List.of());
    }

    private String nestedCondition(int wrappers) {
        String condition = BOOLEAN_PREDICATE;
        for (int index = 0; index < wrappers; index++) {
            condition = "{\"type\":\"ALL\",\"conditions\":[" + condition + "]}";
        }
        return condition;
    }

    private String wideCondition(int children) {
        return "{\"type\":\"ANY\",\"conditions\":["
                + String.join(",", java.util.Collections.nCopies(children, BOOLEAN_PREDICATE))
                + "]}";
    }

    private String setCondition(int values) {
        String elements = IntStream.range(0, values)
                .mapToObj(index -> "\"tag-" + index + "\"")
                .collect(Collectors.joining(","));
        return """
                {"type":"PREDICATE","operator":"IN",
                  "left":{"type":"ATTRIBUTE","name":"subject.name"},
                  "right":{"type":"STRING_SET","values":[%s]}
                }
                """.formatted(elements);
    }

    private com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest
            requestWithSingleAction(String action) {
        PolicyDomainSchema actionSchema = new PolicyDomainSchema(
                "test-1.0",
                List.of(new ActionSchema(action, Set.of("subject.active"), Set.of(), Set.of())),
                List.of(new AttributeSchema("subject.active", PolicyValueType.BOOLEAN, Set.of())),
                List.of());
        return PolicyCompilerTestFixture.request(
                bytes(VALID_BUNDLE),
                Map.of("demo.policy.json", bytes(module(
                        BOOLEAN_PREDICATE, "[\"" + action + "\"]", "[]"))),
                actionSchema);
    }

    private String stringCondition(String value) {
        return """
                {"type":"PREDICATE","operator":"EQ",
                  "left":{"type":"ATTRIBUTE","name":"subject.name"},
                  "right":{"type":"STRING","value":"%s"}
                }
                """.formatted(value);
    }
}
