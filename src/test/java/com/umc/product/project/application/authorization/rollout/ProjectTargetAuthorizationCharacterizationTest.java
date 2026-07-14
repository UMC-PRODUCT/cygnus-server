package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyContextBuilder;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectTargetAuthorizationCharacterizationTest {

    private static final Instant GISU_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-30T23:59:59Z");
    private static final Instant GISU_END = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("동일 snapshot의 target 평가는 effect, outcome, 버전, evaluatedAt이 결정적이다")
    void targetEvaluationIsDeterministicForSameSnapshot() {
        // Given
        var bundle = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        var contextBuilder = new ProjectPolicyContextBuilder();
        var evaluator = new PolicyEvaluationService();
        var snapshot = activeSuperAdminSnapshot();
        var resource = ProjectPolicyResourceContext.builder()
            .project(100L, 99L, 10L, ProjectStatus.IN_PROGRESS)
            .application(200L, ProjectApplicationStatus.SUBMITTED, 30L)
            .build();
        var request = new PolicyEvaluationRequest(
            bundle,
            ProjectPolicyAction.APPLICATION_DECIDE.id(),
            contextBuilder.build(ProjectPolicyAction.APPLICATION_DECIDE, snapshot, resource),
            snapshot.evaluatedAt()
        );

        // When
        PolicyDecision first = (PolicyDecision) evaluator.evaluate(request);
        PolicyDecision second = (PolicyDecision) evaluator.evaluate(request);

        // Then
        assertThat(second).isEqualTo(first);
        assertThat(first.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(first.outcomes()).containsExactly(new PolicyResolvedOutcome(
            ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION,
            new PolicyValue.BooleanValue(true)
        ));
        assertThat(first.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(first.schemaVersion()).isEqualTo("1.0");
        assertThat(first.contextSchemaVersion()).isEqualTo("project-1.0");
        assertThat(first.policyVersion()).isEqualTo("1.1.0");
        assertThat(first.policyFingerprint()).isNotBlank();
    }

    private ProjectPolicySubjectSnapshot activeSuperAdminSnapshot() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(20L),
            EVALUATED_AT,
            true,
            List.of(),
            List.of(),
            Map.of()
        );
    }
}
