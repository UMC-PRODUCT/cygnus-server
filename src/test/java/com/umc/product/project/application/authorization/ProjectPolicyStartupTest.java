package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;

class ProjectPolicyStartupTest {

    @Test
    @DisplayName("Spring startup에서 Project target bundle을 eager compile한다")
    void eagerlyCompilesTargetBundleAtStartup() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PolicySemanticCompiler.class, ProjectPolicyBundleLoader.class);
            context.refresh();

            ProjectPolicyBundleLoader loader = context.getBean(ProjectPolicyBundleLoader.class);
            assertThat(loader.compiled().value().policyFingerprint()).matches("[0-9a-f]{64}");
        }
    }

    @Test
    @DisplayName("role/challenger tuple은 Gisu.isActive와 ChallengerStatus를 정책 입력으로 노출하지 않는다")
    void tuplesDoNotExposeLegacyActivityFlags() {
        assertThat(ProjectPolicyRoleTuple.class.getRecordComponents())
            .extracting(component -> component.getName())
            .doesNotContain("isActive", "challengerStatus", "status");
        assertThat(ProjectPolicyChallengerTuple.class.getRecordComponents())
            .extracting(component -> component.getName())
            .doesNotContain("isActive", "challengerStatus", "status");
    }
}
