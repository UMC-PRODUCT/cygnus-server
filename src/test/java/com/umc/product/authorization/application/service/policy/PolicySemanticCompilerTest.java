package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.BOOLEAN_PREDICATE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.VALID_BUNDLE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.bytes;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.module;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.schema;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

class PolicySemanticCompilerTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("등록된 action과 attribute의 exact type 연산을 컴파일한다")
    @Test
    void compilesRegisteredTypedPolicy() {
        // when
        CompiledPolicyBundle result = compiler.compile(request(VALID_BUNDLE, module(BOOLEAN_PREDICATE)));

        // then
        assertThat(result.schemaVersion()).isEqualTo("1.0");
        assertThat(result.contextSchemaVersion()).isEqualTo("test-1.0");
        assertThat(result.defaultEffect().name()).isEqualTo("DENY");
        assertThat(result.combiningAlgorithm().name()).isEqualTo("DENY_OVERRIDES");
        assertThat(result.modules()).singleElement().satisfies(compiledModule -> {
            assertThat(compiledModule.id()).isEqualTo("demo");
            assertThat(compiledModule.filename()).isEqualTo("demo.policy.json");
            assertThat(compiledModule.statements()).hasSize(1);
        });
    }

    @DisplayName("알 수 없거나 타입이 맞지 않는 policy semantic을 안정적인 코드로 거부한다")
    @MethodSource("invalidSemantics")
    @ParameterizedTest(name = "{0}")
    void rejectsInvalidSemantics(String name, String bundle, String policyModule, PolicyFailureCode expected) {
        // when
        PolicyCompilationException exception = failure(compiler, request(bundle, policyModule));

        // then
        assertThat(exception.code()).isEqualTo(expected);
    }

    @DisplayName("manifest에 선언되지 않은 모듈 입력을 거부한다")
    @Test
    void rejectsUndeclaredModule() {
        // given
        Map<String, byte[]> modules = Map.of(
                "demo.policy.json",
                bytes(module(BOOLEAN_PREDICATE)),
                "extra.policy.json",
                bytes(module(BOOLEAN_PREDICATE)));

        // when
        PolicyCompilationException exception = failure(
                compiler, PolicyCompilerTestFixture.request(bytes(VALID_BUNDLE), modules, schema()));

        // then
        assertThat(exception.code()).isEqualTo(PolicyFailureCode.UNDECLARED_MODULE);
    }

    @DisplayName("manifest에 선언한 모듈 파일이 없으면 거부한다")
    @Test
    void rejectsMissingModule() {
        // when
        PolicyCompilationException exception = failure(
                compiler,
                PolicyCompilerTestFixture.request(bytes(VALID_BUNDLE), Map.of(), schema()));

        // then
        assertThat(exception.code()).isEqualTo(PolicyFailureCode.MISSING_MODULE);
    }

    private static Stream<Arguments> invalidSemantics() {
        String validModule = module(BOOLEAN_PREDICATE);
        String outcome = """
                [{"key":"application.forceDecision","value":{"type":"BOOLEAN","value":true}}]
                """;
        return Stream.of(
                Arguments.of(
                        "unknown action",
                        VALID_BUNDLE,
                        validModule.replace("project:read", "project:unknown"),
                        PolicyFailureCode.UNKNOWN_ACTION),
                Arguments.of(
                        "unknown attribute",
                        VALID_BUNDLE,
                        validModule.replace("subject.active", "subject.unknown"),
                        PolicyFailureCode.UNKNOWN_ATTRIBUTE),
                Arguments.of(
                        "unknown outcome",
                        VALID_BUNDLE,
                        module(BOOLEAN_PREDICATE, "[\"project:read\"]", outcome.replace(
                                "application.forceDecision", "application.unknown")),
                        PolicyFailureCode.UNKNOWN_OUTCOME),
                Arguments.of(
                        "unknown operator",
                        VALID_BUNDLE,
                        validModule.replace("\"EQ\"", "\"MATCHES\""),
                        PolicyFailureCode.UNKNOWN_OPERATOR),
                Arguments.of(
                        "unknown operand type",
                        VALID_BUNDLE,
                        validModule.replace("\"type\":\"BOOLEAN\"", "\"type\":\"DECIMAL\""),
                        PolicyFailureCode.UNKNOWN_OPERAND_TYPE),
                Arguments.of(
                        "operator arity",
                        VALID_BUNDLE,
                        module("""
                                {"type":"PREDICATE","operator":"EQ",
                                  "left":{"type":"ATTRIBUTE","name":"subject.active"}
                                }
                                """),
                        PolicyFailureCode.INVALID_FIELD_TYPE),
                Arguments.of(
                        "operand type mismatch",
                        VALID_BUNDLE,
                        validModule.replace(
                                "{\"type\":\"BOOLEAN\",\"value\":true}",
                                "{\"type\":\"STRING\",\"value\":\"true\"}"),
                        PolicyFailureCode.OPERAND_TYPE_MISMATCH),
                Arguments.of(
                        "module envelope mismatch",
                        VALID_BUNDLE,
                        validModule.replace("\"namespace\": \"test\"", "\"namespace\": \"other\""),
                        PolicyFailureCode.ENVELOPE_MISMATCH),
                Arguments.of(
                        "context schema mismatch",
                        VALID_BUNDLE.replace("test-1.0", "other-1.0"),
                        validModule,
                        PolicyFailureCode.ENVELOPE_MISMATCH),
                Arguments.of(
                        "invalid module filename",
                        VALID_BUNDLE.replace("demo.policy.json", "../demo.policy.json"),
                        validModule,
                        PolicyFailureCode.INVALID_MODULE_FILENAME),
                Arguments.of(
                        "policy version is not semver",
                        VALID_BUNDLE.replace("1.0.0", "latest"),
                        validModule,
                        PolicyFailureCode.INVALID_POLICY_VERSION),
                Arguments.of(
                        "ALLOW default",
                        VALID_BUNDLE.replace("\"defaultEffect\": \"DENY\"", "\"defaultEffect\": \"ALLOW\""),
                        validModule,
                        PolicyFailureCode.ENVELOPE_MISMATCH),
                Arguments.of(
                        "unsupported condition",
                        VALID_BUNDLE,
                        validModule.replace("\"PREDICATE\"", "\"NOT\""),
                        PolicyFailureCode.UNKNOWN_CONDITION_TYPE));
    }
}
