package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.BOOLEAN_PREDICATE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.VALID_BUNDLE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.module;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

class StrictPolicyJsonParserTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("허용하지 않은 JSON 문법과 타입을 안정적인 코드로 거부한다")
    @MethodSource("malformedPolicies")
    @ParameterizedTest(name = "{0}")
    void rejectsMalformedPolicy(String name, String bundle, String policyModule, PolicyFailureCode expected) {
        // when
        PolicyCompilationException exception = failure(compiler, request(bundle, policyModule));

        // then
        assertThat(exception.code()).isEqualTo(expected);
        assertThat(exception.getMessage()).doesNotContain(bundle, policyModule, "sensitive-marker");
    }

    private static Stream<Arguments> malformedPolicies() {
        String validModule = module(BOOLEAN_PREDICATE);
        return Stream.of(
                Arguments.of(
                        "중복 키",
                        VALID_BUNDLE.replace(
                                "\"schemaVersion\": \"1.0\",",
                                "\"schemaVersion\": \"1.0\", \"schemaVersion\": \"1.0\","),
                        validModule,
                        PolicyFailureCode.DUPLICATE_KEY),
                Arguments.of(
                        "알 수 없는 필드",
                        VALID_BUNDLE.replace(
                                "\"modules\":", "\"sensitive-marker\": true, \"modules\":"),
                        validModule,
                        PolicyFailureCode.UNKNOWN_FIELD),
                Arguments.of(
                        "null",
                        VALID_BUNDLE.replace("\"namespace\": \"test\"", "\"namespace\": null"),
                        validModule,
                        PolicyFailureCode.NULL_NOT_ALLOWED),
                Arguments.of(
                        "부동 소수점",
                        VALID_BUNDLE,
                        validModule.replace(
                                "{\"type\":\"BOOLEAN\",\"value\":true}",
                                "{\"type\":\"LONG\",\"value\":1.5}"),
                        PolicyFailureCode.FLOAT_NOT_ALLOWED),
                Arguments.of(
                        "스칼라 강제 변환",
                        VALID_BUNDLE,
                        validModule.replace("\"value\":true", "\"value\":\"true\""),
                        PolicyFailureCode.INVALID_FIELD_TYPE),
                Arguments.of(
                        "주석",
                        "// comment\n" + VALID_BUNDLE,
                        validModule,
                        PolicyFailureCode.MALFORMED_JSON),
                Arguments.of(
                        "후행 토큰",
                        VALID_BUNDLE + "{}",
                        validModule,
                        PolicyFailureCode.TRAILING_CONTENT),
                Arguments.of(
                        "single quote",
                        VALID_BUNDLE.replace('"', '\''),
                        validModule,
                        PolicyFailureCode.MALFORMED_JSON),
                Arguments.of(
                        "default typing hint",
                        VALID_BUNDLE.replace("\"modules\":", "\"@class\": \"sensitive-marker\", \"modules\":"),
                        validModule,
                        PolicyFailureCode.UNKNOWN_FIELD),
                Arguments.of(
                        "지원하지 않는 schema version",
                        VALID_BUNDLE.replace("\"schemaVersion\": \"1.0\"", "\"schemaVersion\": \"1.1\""),
                        validModule,
                        PolicyFailureCode.UNSUPPORTED_SCHEMA_VERSION),
                Arguments.of(
                        "schema version 누락",
                        VALID_BUNDLE.replace("\"schemaVersion\": \"1.0\",", ""),
                        validModule,
                        PolicyFailureCode.MISSING_SCHEMA_FIELD),
                Arguments.of(
                        "schema version 숫자 타입",
                        VALID_BUNDLE.replace("\"schemaVersion\": \"1.0\"", "\"schemaVersion\": 1"),
                        validModule,
                        PolicyFailureCode.INVALID_FIELD_TYPE));
    }
}
