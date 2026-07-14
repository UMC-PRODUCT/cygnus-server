package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.VALID_BUNDLE;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.module;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

class PolicyOperatorSignatureTest {

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("지원 operator의 exact operand signature를 컴파일한다")
    @MethodSource("validSignatures")
    @ParameterizedTest(name = "{0}")
    void compilesExactOperatorSignature(String operator, String condition) {
        assertThatCode(() -> compiler.compile(request(VALID_BUNDLE, module(condition))))
                .doesNotThrowAnyException();
    }

    @DisplayName("enum/string 혼합과 잘못된 set 방향을 거부한다")
    @MethodSource("invalidSignatures")
    @ParameterizedTest(name = "{0}")
    void rejectsInvalidOperatorSignature(String name, String condition) {
        PolicyCompilationException exception = failure(compiler, request(VALID_BUNDLE, module(condition)));
        assertThat(exception.code()).isEqualTo(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
    }

    private static Stream<Arguments> validSignatures() {
        return Stream.of(
                Arguments.of("EQ", predicate("EQ", attribute("subject.active"), literal("BOOLEAN", "true"))),
                Arguments.of("NEQ", predicate("NEQ", attribute("subject.age"), literal("LONG", "20"))),
                Arguments.of(
                        "IN",
                        predicate(
                                "IN",
                                attribute("subject.name"),
                                literal("STRING_SET", "[\"alpha\",\"beta\"]"))),
                Arguments.of(
                        "CONTAINS",
                        predicate(
                                "CONTAINS", attribute("subject.tags"), literal("STRING", "\"alpha\""))),
                Arguments.of(
                        "INTERSECTS",
                        predicate(
                                "INTERSECTS",
                                attribute("subject.tags"),
                                literal("STRING_SET", "[\"alpha\"]"))),
                Arguments.of("EXISTS", predicate("EXISTS", attribute("subject.name"))),
                Arguments.of("NOT_EXISTS", predicate("NOT_EXISTS", attribute("subject.name"))),
                Arguments.of("LT", predicate("LT", attribute("subject.age"), literal("LONG", "20"))),
                Arguments.of("LTE", predicate("LTE", attribute("subject.age"), literal("LONG", "20"))),
                Arguments.of("GT", predicate("GT", attribute("subject.age"), literal("LONG", "20"))),
                Arguments.of("GTE", predicate("GTE", attribute("subject.age"), literal("LONG", "20"))));
    }

    private static Stream<Arguments> invalidSignatures() {
        return Stream.of(
                Arguments.of(
                        "string과 enum은 혼합하지 않는다",
                        predicate("EQ", attribute("subject.name"), literal("ENUM", "\"alpha\""))),
                Arguments.of(
                        "IN은 scalar 다음 동일 set만 허용한다",
                        predicate(
                                "IN", attribute("subject.tags"), literal("STRING", "\"alpha\""))),
                Arguments.of(
                        "CONTAINS는 set 다음 동일 scalar만 허용한다",
                        predicate(
                                "CONTAINS",
                                attribute("subject.name"),
                                literal("STRING_SET", "[\"alpha\"]"))));
    }

    private static String predicate(String operator, String... operands) {
        if (operands.length == 1) {
            return "{\"type\":\"PREDICATE\",\"operator\":\""
                    + operator
                    + "\",\"left\":"
                    + operands[0]
                    + "}";
        }
        return "{\"type\":\"PREDICATE\",\"operator\":\""
                + operator
                + "\",\"left\":"
                + operands[0]
                + ",\"right\":"
                + operands[1]
                + "}";
    }

    private static String attribute(String name) {
        return "{\"type\":\"ATTRIBUTE\",\"name\":\"" + name + "\"}";
    }

    private static String literal(String type, String value) {
        String valueField = type.endsWith("_SET") ? "values" : "value";
        return "{\"type\":\"" + type + "\",\"" + valueField + "\":" + value + "}";
    }
}
