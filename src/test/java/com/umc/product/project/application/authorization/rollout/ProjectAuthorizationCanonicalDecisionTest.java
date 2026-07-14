package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;

class ProjectAuthorizationCanonicalDecisionTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    @DisplayName("정책 outcome을 typed scope, form, obligation으로 canonicalize한다")
    void policyOutcomesAreCanonicalized() {
        // Given
        PolicyDecision policyDecision = new PolicyDecision(
            PolicyEffect.ALLOW,
            List.of("allow"),
            List.of(),
            List.of(
                outcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, new PolicyValue.BooleanValue(true)),
                outcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, new PolicyValue.LongSetValue(Set.of(30L, 10L))),
                outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS, new PolicyValue.LongSetValue(Set.of(90L))),
                outcome(ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS,
                    new PolicyValue.BooleanValue(true)),
                outcome(ProjectPolicyOutcomes.FORM_VIEW, new PolicyValue.EnumValue("APPLICANT")),
                outcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION, new PolicyValue.BooleanValue(true)),
                outcome("audit.zeta", new PolicyValue.StringValue("z")),
                outcome("audit.alpha", new PolicyValue.StringValue("a"))
            ),
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "fingerprint"
        );

        // When
        ProjectAuthorizationDecision decision = ProjectAuthorizationDecision.fromPolicyDecision(
            ProjectPolicyAction.FORM_READ,
            policyDecision
        );

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(decision.projectScope()).isEqualTo(new ProjectAuthorizationProjectScope(
            false, true, Set.of(), Set.of(10L, 30L), Set.of(), false));
        assertThat(decision.applicationScope()).isEqualTo(new ProjectAuthorizationApplicationScope(
            false, Set.of(), Set.of(), Set.of(90L), Set.of(), true));
        assertThat(decision.formView()).isEqualTo(ProjectAuthorizationFormView.APPLICANT);
        assertThat(decision.forceDecision()).isTrue();
        assertThat(decision.capabilityAuthorized()).isTrue();
        assertThat(decision.obligations())
            .extracting(ProjectAuthorizationObligation::key)
            .containsExactly("audit.alpha", "audit.zeta");
    }

    @Test
    @DisplayName("nullable caller는 comparison request로 표현할 수 없고 typed SYSTEM만 보존한다")
    void comparisonRequestRejectsNullCallerAndPreservesTypedSystem() {
        // Given
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .matchingRound(77L, 1L, 10L)
            .build();
        var point = ProjectAuthorizationEvaluationPoint.internal(
            ProjectAuthorizationInternalOrigin.MATCHING_SCHEDULER
        );

        // When & Then
        assertThatThrownBy(() -> new ProjectAuthorizationComparisonRequest(
            null,
            ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            resource,
            point
        )).isInstanceOf(NullPointerException.class);

        ProjectPolicySubjectSnapshot system = ProjectPolicySubjectSnapshot.system(
            "matching-round-scheduler",
            EVALUATED_AT
        );
        ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
            system,
            ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            resource,
            point
        );
        assertThat(request.snapshot()).isSameAs(system);
        assertThat(request.evaluatedAt()).isEqualTo(EVALUATED_AT);
    }

    @Test
    @DisplayName("surface evaluation point는 catalog identity와 action을 exact 검증한다")
    void surfaceEvaluationPointRequiresCatalogIdentityAndMatchingAction() {
        // Given
        var point = ProjectAuthorizationEvaluationPoint.surface(
            ProjectAuthorizationSurface.GRAPHQL_APPLICATION_FORM);
        var request = new ProjectAuthorizationComparisonRequest(
            memberSnapshot(),
            ProjectPolicyAction.FORM_READ,
            ProjectPolicyResourceContext.builder().projectTarget(1L, 10L).build(),
            point
        );

        // When & Then
        assertThat(request.evaluationPoint()).isEqualTo(point);
        assertThatThrownBy(() -> new ProjectAuthorizationComparisonRequest(
            memberSnapshot(),
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder().projectTarget(1L, 10L).build(),
            point
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("trusted resource snapshot은 target member school fact를 policy context와 함께 보존한다")
    void resourceSnapshotPreservesTrustedTargetSchoolFact() {
        // Given
        ProjectPolicyResourceContext policyContext = ProjectPolicyResourceContext.builder()
            .projectTarget(1L, 10L)
            .creatorMemberId(90L)
            .build();

        // When
        ProjectAuthorizationResourceSnapshot resource =
            ProjectAuthorizationResourceSnapshot.withTargetMemberSchool(policyContext, 300L);

        // Then
        assertThat(resource.policyContext()).isSameAs(policyContext);
        assertThat(resource.targetMemberSchoolId()).contains(300L);
    }

    @Test
    @DisplayName("trusted target chapter 학교 집합은 defensive copy와 canonical 정렬을 보장한다")
    void targetChapterSchoolIdsAreDefensivelyCopiedAndSorted() {
        // Given
        ProjectPolicyResourceContext policyContext = ProjectPolicyResourceContext.builder()
            .chapterScope(1L, 10L)
            .build();
        Set<Long> mutableSchoolIds = new HashSet<>(Set.of(30L, 10L));

        // When
        ProjectAuthorizationResourceSnapshot resource =
            ProjectAuthorizationResourceSnapshot.withTargetChapterSchools(policyContext, mutableSchoolIds);
        mutableSchoolIds.add(20L);

        // Then
        assertThat(resource.targetChapterSchoolIds()).containsExactly(10L, 30L);
        assertThatThrownBy(() -> resource.targetChapterSchoolIds().add(40L))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private ProjectPolicySubjectSnapshot memberSnapshot() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), EVALUATED_AT, List.of(), List.of(), Map.of());
    }

    private PolicyResolvedOutcome outcome(String key, PolicyValue value) {
        return new PolicyResolvedOutcome(key, value);
    }
}
