package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectExpectedDifferenceMatrixTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private final ProjectExpectedDifferenceMatrix matrix = InitialProjectExpectedDifferenceMatrix.create();

    @Test
    @DisplayName("E001은 resource scope가 일치하는 만료 staff의 exact allow-to-deny만 분류한다")
    void expiredStaffExactDecisionPairMatchesE001() {
        ProjectAuthorizationComparisonRequest request = projectRead(expiredCentralCore());

        Optional<ProjectExpectedDifferenceId> matched = matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied());
        ProjectAuthorizationDecision differentOutcome = new ProjectAuthorizationDecision(
            PolicyEffect.ALLOW,
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE,
            false,
            true,
            List.of(new ProjectAuthorizationObligation("audit.extra", new PolicyValue.BooleanValue(true)))
        );

        assertThat(matched).contains(ProjectExpectedDifferenceId.E001);
        assertThat(matrix.expectedDifferenceId(request, differentOutcome, ProjectAuthorizationDecision.denied()))
            .isEmpty();
    }

    @Test
    @DisplayName("E011은 runtime typed SYSTEM 요청을 분류하지 않고 synthetic witness로만 보존한다")
    void nullableSchedulerDifferenceIsSyntheticOnly() {
        ProjectAuthorizationComparisonRequest forged = systemRequest("forged-scheduler");
        ProjectAuthorizationComparisonRequest scheduler = systemRequest("matching-round-scheduler");
        LoadedProjectExpectedDifferenceMatrix loaded = (LoadedProjectExpectedDifferenceMatrix) matrix;

        assertThat(matrix.expectedDifferenceId(
            forged, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
        assertThat(matrix.expectedDifferenceId(
            scheduler, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
        assertThat(loaded.hasSyntheticWitness(
            ProjectExpectedDifferenceId.E011, ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE)).isTrue();
    }

    @Test
    @DisplayName("만료 staff가 있어도 member batch는 E001로 분류하지 않는다")
    void expiredStaffDoesNotClassifyMemberBatchAsE001() {
        ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
            expiredCentralCore(),
            ProjectPolicyAction.PROJECT_MEMBER_BATCH,
            ProjectPolicyResourceContext.builder().build(),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR)
        );

        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
    }

    @Test
    @DisplayName("tracked matrix는 E001 40행과 전체 58행을 가진다")
    void exactRowInventoryIsLoaded() {
        LoadedProjectExpectedDifferenceMatrix loaded = (LoadedProjectExpectedDifferenceMatrix) matrix;

        assertThat(loaded.rowCount()).isEqualTo(58);
        assertThat(loaded.rowCount(ProjectExpectedDifferenceId.E001)).isEqualTo(40);
        assertThat(java.util.Arrays.stream(ProjectExpectedDifferenceId.values())
            .allMatch(id -> loaded.rowCount(id) > 0)).isTrue();
    }

    @Test
    @DisplayName("모든 expected difference 행은 unique id와 exact action을 가진다")
    void rowsHaveUniqueIdsAndExactActions() {
        LoadedProjectExpectedDifferenceMatrix loaded = (LoadedProjectExpectedDifferenceMatrix) matrix;

        assertThat(loaded.entries()).extracting(ProjectExpectedDifferenceWire.Entry::id)
            .doesNotHaveDuplicates();
        assertThat(loaded.entries()).allSatisfy(entry -> {
            assertThat(entry.id()).isNotBlank();
            assertThat(entry.action().id()).doesNotContain("*", "?");
        });
    }

    private ProjectAuthorizationComparisonRequest projectRead(ProjectPolicySubjectSnapshot subject) {
        return new ProjectAuthorizationComparisonRequest(
            subject,
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder()
                .project(10L, 1L, 20L, ProjectStatus.PENDING_REVIEW)
                .build(),
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR)
        );
    }

    private ProjectPolicySubjectSnapshot expiredCentralCore() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L),
            EXPIRED_AT,
            List.of(new ProjectPolicyRoleTuple(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                1L,
                START,
                END
            )),
            List.of(),
            Map.of()
        );
    }

    private ProjectAuthorizationComparisonRequest systemRequest(String systemId) {
        return new ProjectAuthorizationComparisonRequest(
            ProjectPolicySubjectSnapshot.system(systemId, EXPIRED_AT),
            ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            ProjectPolicyResourceContext.builder().matchingRound(10L, 1L, 20L).build(),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.MATCHING_SCHEDULER)
        );
    }
}
