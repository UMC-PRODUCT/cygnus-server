package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
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
import com.umc.product.authorization.domain.policy.PolicyValueType;

class ApprovedPolicyWireContractTest {

    private static final String APPROVED_BUNDLE = "policies/task-2/approved/bundle.policy.json";
    private static final String APPROVED_MODULE =
            "policies/task-2/approved/project-resource.policy.json";
    private static final String LEGACY_BUNDLE = "policies/task-2/legacy/bundle.policy.json";
    private static final String LEGACY_MODULE =
            "policies/task-2/legacy/project-resource.policy.json";

    private final PolicySemanticCompiler compiler = new PolicySemanticCompiler();

    @DisplayName("승인된 schema 1.0 wire bundle과 ATTRIBUTE outcome을 컴파일한다")
    @Test
    void compilesApprovedSchema10Wire() throws Exception {
        assertThatCode(() -> {
                    CompiledPolicyBundle compiled = compiler.compile(request(APPROVED_BUNDLE, APPROVED_MODULE));
                    assertThat(compiled.modules()).singleElement().satisfies(module -> assertThat(module.statements())
                            .singleElement()
                            .satisfies(statement -> assertThat(statement.outcomes()).hasSize(1)));
                })
                .doesNotThrowAnyException();
    }

    @DisplayName("구현 고유 file operands dialect를 UNKNOWN_FIELD로 거부한다")
    @Test
    void rejectsLegacyImplementationDialect() throws Exception {
        assertThatThrownBy(() -> compiler.compile(request(LEGACY_BUNDLE, LEGACY_MODULE)))
                .isInstanceOfSatisfying(PolicyCompilationException.class, exception -> assertThat(exception.code())
                        .isEqualTo(PolicyFailureCode.UNKNOWN_FIELD));
    }

    private PolicyBundleCompilationRequest request(String bundle, String module) throws Exception {
        return new PolicyBundleCompilationRequest(
                resource(bundle), Map.of("project-resource.policy.json", resource(module)), domainSchema());
    }

    private PolicyDomainSchema domainSchema() throws Exception {
        ActionSchema action = actionSchema();
        AttributeSchema active = attributeSchema("subject.active", PolicyValueType.BOOLEAN);
        AttributeSchema managedGisuIds = attributeSchema("relation.managedGisuIds", PolicyValueType.LONG_SET);
        OutcomeSchema outcome = new OutcomeSchema(
                "project.scope.gisuIds",
                PolicyValueType.LONG_SET,
                OutcomeMergeStrategy.SET_UNION,
                List.of());
        return new PolicyDomainSchema(
                "test-1.0", List.of(action), List.of(active, managedGisuIds), List.of(outcome));
    }

    private ActionSchema actionSchema() throws Exception {
        try {
            Constructor<ActionSchema> constructor = ActionSchema.class.getConstructor(
                    String.class, Set.class, Set.class, Set.class);
            return constructor.newInstance(
                    "project:list",
                    Set.of("subject.active", "relation.managedGisuIds"),
                    Set.of(),
                    Set.of("project.scope.gisuIds"));
        } catch (NoSuchMethodException exception) {
            return ActionSchema.class.getConstructor(String.class).newInstance("project:list");
        }
    }

    private AttributeSchema attributeSchema(String name, PolicyValueType type) throws Exception {
        try {
            Constructor<AttributeSchema> constructor =
                    AttributeSchema.class.getConstructor(String.class, PolicyValueType.class, Set.class);
            return constructor.newInstance(name, type, Set.of());
        } catch (NoSuchMethodException exception) {
            return AttributeSchema.class
                    .getConstructor(String.class, PolicyValueType.class, boolean.class)
                    .newInstance(name, type, true);
        }
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Policy test resource is missing");
            }
            return input.readAllBytes();
        }
    }
}
