package com.umc.product.project.application.authorization.rollout.legacy;

import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.CHAPTER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.GISU_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.SCHOOL_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.internal;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.member;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.role;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.domain.enums.ProjectStatus;

class LegacyProjectStatisticsTrustedResourceTest {

    private static final long OTHER_GISU_ID = 99L;
    private static final long OTHER_SCHOOL_ID = 31L;

    private final LegacyProjectAuthorizationAdapter adapter = new LegacyProjectAuthorizationAdapter();

    @Test
    @DisplayName("legacy project statistics는 역할 기수와 무관하게 target chapter 학교 회장단을 허용한다")
    void projectStatisticsUsesTrustedTargetChapterSchools() {
        assertMatchedSchoolAllowedAndNearMissDenied(ProjectPolicyAction.STATISTICS_PROJECT);
    }

    @Test
    @DisplayName("legacy chapter statistics는 역할 기수와 무관하게 target chapter 학교 회장단을 허용한다")
    void chapterStatisticsUsesTrustedTargetChapterSchools() {
        assertMatchedSchoolAllowedAndNearMissDenied(ProjectPolicyAction.STATISTICS_CHAPTER);
    }

    private void assertMatchedSchoolAllowedAndNearMissDenied(ProjectPolicyAction action) {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, OTHER_GISU_ID)),
            List.of(),
            Map.of()
        );
        ProjectPolicyResourceContext policyContext = ProjectPolicyResourceContext.builder()
            .project(100L, GISU_ID, CHAPTER_ID, ProjectStatus.IN_PROGRESS)
            .productOwnerMemberId(999L)
            .build();

        // When
        ProjectAuthorizationDecision matched = evaluate(
            action,
            subject,
            ProjectAuthorizationResourceSnapshot.withTargetChapterSchools(
                policyContext, Set.of(SCHOOL_ID))
        );
        ProjectAuthorizationDecision nearMiss = evaluate(
            action,
            subject,
            ProjectAuthorizationResourceSnapshot.withTargetChapterSchools(
                policyContext, Set.of(OTHER_SCHOOL_ID))
        );

        // Then
        assertThat(matched.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(nearMiss.effect()).isEqualTo(PolicyEffect.DENY);
    }

    private ProjectAuthorizationDecision evaluate(
        ProjectPolicyAction action,
        com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot subject,
        ProjectAuthorizationResourceSnapshot resource
    ) {
        return (ProjectAuthorizationDecision) adapter.evaluate(
            new ProjectAuthorizationComparisonRequest(subject, action, resource, internal()));
    }
}
