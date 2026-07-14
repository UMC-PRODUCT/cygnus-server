package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.BOOLEAN_PREDICATE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.bytes;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

class PolicySemanticCompilerCanonicalizationTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("역순 manifest의 module도 누락 없이 컴파일한다")
    @Test
    void compilesReversedManifestWithoutLosingModules() {
        // given
        String bundle = bundleWithReversedModules();

        // when
        CompiledPolicyBundle compiled = compiler.compile(request(
                bytes(bundle),
                Map.of(
                        "alpha.policy.json", bytes(module("alpha", "alpha.allow")),
                        "beta.policy.json", bytes(module("beta", "beta.allow")))));

        // then
        assertThat(compiled.modules()).extracting(module -> module.id()).containsExactlyInAnyOrder("beta", "alpha");
    }

    @DisplayName("compiled AST의 module은 source 순서와 무관하게 ID 순으로 정규화한다")
    @Test
    void canonicalizesModulesById() {
        // given
        String bundle = bundleWithReversedModules();

        // when
        CompiledPolicyBundle compiled = compiler.compile(request(
                bytes(bundle),
                Map.of(
                        "alpha.policy.json", bytes(module("alpha", "alpha.allow")),
                        "beta.policy.json", bytes(module("beta", "beta.allow")))));

        // then
        assertThat(compiled.modules()).extracting(module -> module.id()).containsExactly("alpha", "beta");
    }

    private String bundleWithReversedModules() {
        return """
                {
                  "$schema":"urn:umc:authorization:policy-bundle:1.0",
                  "schemaVersion":"1.0",
                  "contextSchemaVersion":"test-1.0",
                  "namespace":"test",
                  "policyVersion":"1.0.0",
                  "defaultEffect":"DENY",
                  "combiningAlgorithm":"DENY_OVERRIDES",
                  "modules":[
                    {"id":"beta","resource":"beta.policy.json"},
                    {"id":"alpha","resource":"alpha.policy.json"}
                  ]
                }
                """;
    }

    private String module(String moduleId, String statementId) {
        return """
                {
                  "$schema":"urn:umc:authorization:policy-module:1.0",
                  "schemaVersion":"1.0",
                  "contextSchemaVersion":"test-1.0",
                  "namespace":"test",
                  "policyVersion":"1.0.0",
                  "moduleId":"%s",
                  "statements":[{
                    "id":"%s",
                    "actions":["project:read"],
                    "effect":"ALLOW",
                    "condition":%s,
                    "outcomes":[]
                  }]
                }
                """
                .formatted(moduleId, statementId, BOOLEAN_PREDICATE);
    }
}
