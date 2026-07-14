package com.umc.product.authorization.application.service.policy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValueType;

final class PolicyCompilerTestFixture {

    static final String VALID_BUNDLE = """
            {
              "$schema": "urn:umc:authorization:policy-bundle:1.0",
              "schemaVersion": "1.0",
              "contextSchemaVersion": "test-1.0",
              "namespace": "test",
              "policyVersion": "1.0.0",
              "defaultEffect": "DENY",
              "combiningAlgorithm": "DENY_OVERRIDES",
              "modules": [{"id": "demo", "resource": "demo.policy.json"}]
            }
            """;

    static final String BOOLEAN_PREDICATE = """
            {"type":"PREDICATE","operator":"EQ",
              "left":{"type":"ATTRIBUTE","name":"subject.active"},
              "right":{"type":"BOOLEAN","value":true}
            }
            """;

    private PolicyCompilerTestFixture() {}

    static String module(String condition) {
        return module(condition, "[\"project:read\"]", "[]");
    }

    static String module(String condition, String actions, String outcomes) {
        return """
                {
                  "$schema": "urn:umc:authorization:policy-module:1.0",
                  "schemaVersion": "1.0",
                  "contextSchemaVersion": "test-1.0",
                  "namespace": "test",
                  "policyVersion": "1.0.0",
                  "moduleId": "demo",
                  "statements": [{
                    "id": "allow-active",
                    "actions": %s,
                    "effect": "ALLOW",
                    "condition": %s,
                    "outcomes": %s
                  }]
                }
                """
                .formatted(actions, condition, outcomes);
    }

    static PolicyBundleCompilationRequest request(String bundle, String module) {
        return request(bytes(bundle), Map.of("demo.policy.json", bytes(module)), schema());
    }

    static PolicyBundleCompilationRequest request(byte[] bundle, Map<String, byte[]> modules) {
        return request(bundle, modules, schema());
    }

    static PolicyBundleCompilationRequest request(
            byte[] bundle, Map<String, byte[]> modules, PolicyDomainSchema schema) {
        return new PolicyBundleCompilationRequest(bundle, modules, schema);
    }

    static PolicyDomainSchema schema() {
        return new PolicyDomainSchema(
                "test-1.0",
                List.of(new ActionSchema(
                        "project:read",
                        Set.of("subject.active"),
                        Set.of("subject.age", "subject.name", "subject.tags"),
                        Set.of("application.forceDecision"))),
                List.of(
                        new AttributeSchema("subject.active", PolicyValueType.BOOLEAN, Set.of()),
                        new AttributeSchema("subject.age", PolicyValueType.LONG, Set.of()),
                        new AttributeSchema("subject.name", PolicyValueType.STRING, Set.of()),
                        new AttributeSchema("subject.tags", PolicyValueType.STRING_SET, Set.of())),
                List.of(new OutcomeSchema(
                        "application.forceDecision",
                        PolicyValueType.BOOLEAN,
                        OutcomeMergeStrategy.BOOLEAN_OR,
                        List.of())));
    }

    static PolicyCompilationException failure(
            PolicySemanticCompiler compiler, PolicyBundleCompilationRequest request) {
        try {
            compiler.compile(request);
            throw new AssertionError("정책 컴파일이 실패해야 한다");
        } catch (PolicyCompilationException exception) {
            return exception;
        }
    }

    static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
