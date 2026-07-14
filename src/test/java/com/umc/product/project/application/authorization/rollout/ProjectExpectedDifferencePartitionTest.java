package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectExpectedDifferencePartitionTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2027-01-01T00:00:00Z");
    private final ProjectExpectedDifferenceMatrix matrix = InitialProjectExpectedDifferenceMatrix.create();

    @Test
    @DisplayName("E001 expired witness를 active로 바꾸면 approved difference가 아니다")
    void activeRoleDoesNotMatchExpiredPartition() {
        ProjectAuthorizationComparisonRequest request = request(
            ProjectPolicyAction.PROJECT_READ,
            List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, 2L, null, true)),
            List.of()
        );

        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
    }

    @Test
    @DisplayName("E003 project create witness는 E001과 분리된다")
    void createHistoryMatchesOnlyE003() {
        ProjectPolicyChallengerTuple otherGisuPlan = new ProjectPolicyChallengerTuple(
            1L, 2L, 30L, ChallengerPart.PLAN, START, END);
        ProjectAuthorizationComparisonRequest request = request(
            ProjectPolicyAction.PROJECT_CREATE, List.of(), List.of(otherGisuPlan));

        assertThat(ProjectExpectedDifferenceFacts.booleanValue(
            "difference.expiredStaffForAction", request)).isFalse();
        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied()))
            .contains(ProjectExpectedDifferenceId.E003);
    }

    @Test
    @DisplayName("E007 other-gisu active statistics witness는 E001과 분리된다")
    void statisticsOtherGisuMatchesOnlyE007() {
        ProjectAuthorizationComparisonRequest request = request(
            ProjectPolicyAction.STATISTICS_PROJECT,
            List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, 2L, null, true)),
            List.of()
        );

        assertThat(ProjectExpectedDifferenceFacts.booleanValue(
            "difference.expiredStaffForAction", request)).isFalse();
        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied()))
            .contains(ProjectExpectedDifferenceId.E007);
    }

    @Test
    @DisplayName("E008 other-gisu active matching witness는 E001과 분리된다")
    void matchingOtherGisuMatchesOnlyE008() {
        ProjectAuthorizationComparisonRequest request = request(
            ProjectPolicyAction.MATCHING_CREATE,
            List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, 2L, null, true)),
            List.of()
        );

        assertThat(ProjectExpectedDifferenceFacts.booleanValue(
            "difference.expiredStaffForAction", request)).isFalse();
        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied()))
            .contains(ProjectExpectedDifferenceId.E008);
    }

    @Test
    @DisplayName("expired witness라도 action을 member batch로 바꾸면 E001이 아니다")
    void actionNearMissDoesNotMatchE001() {
        ProjectAuthorizationComparisonRequest request = request(
            ProjectPolicyAction.PROJECT_MEMBER_BATCH,
            List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, 2L, null, false)),
            List.of()
        );

        assertThat(matrix.expectedDifferenceId(
            request, ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
    }

    private ProjectAuthorizationComparisonRequest request(
        ProjectPolicyAction action,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers
    ) {
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), NOW, roles, challengers, Map.of());
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .project(10L, 1L, 20L, ProjectStatus.PENDING_REVIEW)
            .matchingRound(40L, 1L, 20L)
            .creatorMemberId(1L)
            .productOwnerMemberId(2L)
            .build();
        ProjectAuthorizationInternalOrigin origin = switch (action) {
            case STATISTICS_PROJECT -> ProjectAuthorizationInternalOrigin.STATISTICS_ACCESS_POLICY;
            case MATCHING_CREATE -> ProjectAuthorizationInternalOrigin.MATCHING_COMMAND;
            default -> ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR;
        };
        return new ProjectAuthorizationComparisonRequest(
            subject, action, resource, ProjectAuthorizationEvaluationPoint.internal(origin));
    }

    private ProjectPolicyRoleTuple role(
        ChallengerRoleType type,
        long gisuId,
        Long organizationId,
        boolean active
    ) {
        return new ProjectPolicyRoleTuple(
            type, OrganizationType.CENTRAL, organizationId, null, gisuId,
            START, active ? END : NOW);
    }
}
