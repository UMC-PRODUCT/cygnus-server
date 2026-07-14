package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyEngineBehaviorContractTest {

    private static final String VALID_BUNDLE = """
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

    private static final String VALID_MODULE = """
            {
              "$schema": "urn:umc:authorization:policy-module:1.0",
              "schemaVersion": "1.0",
              "contextSchemaVersion": "test-1.0",
              "namespace": "test",
              "policyVersion": "1.0.0",
              "moduleId": "demo",
              "statements": [{
                "id": "allow-active",
                "actions": ["project:read"],
                "effect": "ALLOW",
                "condition": {
                  "type": "PREDICATE",
                  "operator": "EQ",
                  "left": {"type": "ATTRIBUTE", "name": "subject.active"},
                  "right": {"type": "BOOLEAN", "value": true}
                },
                "outcomes": []
              }]
            }
            """;

    @DisplayName("schema 1.0의 최소 정책 번들을 typed compiler로 컴파일한다")
    @Test
    void compilesMinimalSchema10Bundle() {
        assertThatCode(() -> {
                    // given
                    Object compiler = newCompiler();
                    Object request = compilationRequest(
                            VALID_BUNDLE, Map.of("demo.policy.json", bytes(VALID_MODULE)));

                    // when
                    Object compiled = compile(compiler, request);

                    // then
                    assertThat(compiled).isNotNull();
                })
                .doesNotThrowAnyException();
    }

    @DisplayName("중복 키를 안정적인 코드로 거부한다")
    @Test
    void rejectsDuplicateKeyWithStableCode() throws Exception {
        // given
        String duplicate = VALID_BUNDLE.replace(
                "\"schemaVersion\": \"1.0\",", "\"schemaVersion\": \"1.0\", \"schemaVersion\": \"1.0\",");

        // when
        Throwable failure = compileFailure(duplicate, Map.of("demo.policy.json", bytes(VALID_MODULE)));

        // then
        assertThat(failureCode(failure)).isEqualTo("DUPLICATE_KEY");
    }

    @DisplayName("알 수 없는 필드를 안정적인 코드로 거부하고 원문을 노출하지 않는다")
    @Test
    void rejectsUnknownFieldWithoutSourceLeak() throws Exception {
        // given
        String unknown = VALID_BUNDLE.replace(
                "\"modules\":", "\"sensitive-marker\": \"private-value\", \"modules\":");

        // when
        Throwable failure = compileFailure(unknown, Map.of("demo.policy.json", bytes(VALID_MODULE)));

        // then
        assertThat(failureCode(failure)).isEqualTo("UNKNOWN_FIELD");
        assertThat(failure.getMessage()).doesNotContain("sensitive-marker", "private-value", unknown);
    }

    private Object newCompiler() throws Exception {
        Class<?> compilerClass = Class.forName(
                "com.umc.product.authorization.application.service.policy.PolicySemanticCompiler");
        return compilerClass.getConstructor().newInstance();
    }

    private Object compilationRequest(String bundle, Map<String, byte[]> modules) throws Exception {
        Class<?> actionSchemaClass = Class.forName(
                "com.umc.product.authorization.domain.policy.ActionSchema");
        Object actionSchema = actionSchemaClass
                .getConstructor(String.class, Set.class, Set.class, Set.class)
                .newInstance("project:read", Set.of("subject.active"), Set.of(), Set.of());

        Class<?> valueTypeClass = Class.forName(
                "com.umc.product.authorization.domain.policy.PolicyValueType");
        Object booleanType = enumValue(valueTypeClass, "BOOLEAN");
        Class<?> attributeSchemaClass = Class.forName(
                "com.umc.product.authorization.domain.policy.AttributeSchema");
        Object attributeSchema = attributeSchemaClass
                .getConstructor(String.class, valueTypeClass, Set.class)
                .newInstance("subject.active", booleanType, Set.of());

        Class<?> domainSchemaClass = Class.forName(
                "com.umc.product.authorization.domain.policy.PolicyDomainSchema");
        Constructor<?> schemaConstructor =
                domainSchemaClass.getConstructor(String.class, List.class, List.class, List.class);
        Object domainSchema = schemaConstructor.newInstance(
                "test-1.0", List.of(actionSchema), List.of(attributeSchema), List.of());

        Class<?> requestClass = Class.forName(
                "com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest");
        return requestClass
                .getConstructor(byte[].class, Map.class, domainSchemaClass)
                .newInstance(bytes(bundle), modules, domainSchema);
    }

    private Object compile(Object compiler, Object request) throws Exception {
        Method compile = compiler.getClass().getMethod("compile", request.getClass());
        return compile.invoke(compiler, request);
    }

    private Throwable compileFailure(String bundle, Map<String, byte[]> modules) throws Exception {
        Object compiler = newCompiler();
        Object request = compilationRequest(bundle, modules);
        try {
            compile(compiler, request);
            throw new AssertionError("정책 컴파일이 실패해야 한다");
        } catch (InvocationTargetException exception) {
            return exception.getCause();
        }
    }

    private String failureCode(Throwable failure) throws Exception {
        Object code = failure.getClass().getMethod("code").invoke(failure);
        return ((Enum<?>) code).name();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, name);
    }

    private byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
