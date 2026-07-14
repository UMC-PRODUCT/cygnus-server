package com.umc.product.project.application.authorization.rollout.legacy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

final class LegacyProjectAuthorizationFixture {

    static final long MEMBER_ID = 50L;
    static final long OTHER_MEMBER_ID = 60L;
    static final long GISU_ID = 10L;
    static final long CHAPTER_ID = 20L;
    static final long SCHOOL_ID = 30L;
    static final Instant STARTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant EXPIRED_AT = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant EVALUATED_AT = Instant.parse("2026-07-02T00:00:00Z");

    private LegacyProjectAuthorizationFixture() {}

    static ProjectPolicySubjectSnapshot member(
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> schoolChapters
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID),
            EVALUATED_AT,
            roles,
            challengers,
            schoolChapters
        );
    }

    static ProjectPolicySubjectSnapshot superAdminMember(
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> schoolChapters
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID),
            EVALUATED_AT,
            true,
            roles,
            challengers,
            schoolChapters
        );
    }

    static ProjectPolicySubjectSnapshot system(String systemId) {
        return ProjectPolicySubjectSnapshot.system(systemId, EVALUATED_AT);
    }

    static ProjectPolicyRoleTuple role(
        ChallengerRoleType type,
        Long organizationId,
        long gisuId
    ) {
        return new ProjectPolicyRoleTuple(
            type,
            organizationType(type),
            organizationId,
            null,
            gisuId,
            STARTED_AT,
            EXPIRED_AT
        );
    }

    static ProjectPolicyChallengerTuple challenger(long gisuId, ChallengerPart part) {
        return new ProjectPolicyChallengerTuple(
            70L, gisuId, CHAPTER_ID, part, STARTED_AT, EXPIRED_AT);
    }

    static ProjectPolicyResourceContext richResource() {
        return ProjectPolicyResourceContext.builder()
            .project(100L, GISU_ID, CHAPTER_ID, ProjectStatus.IN_PROGRESS)
            .application(200L, ProjectApplicationStatus.SUBMITTED, OTHER_MEMBER_ID)
            .matchingRound(300L, GISU_ID, CHAPTER_ID)
            .creatorMemberId(MEMBER_ID)
            .productOwnerMemberId(MEMBER_ID)
            .activePlanMember(true)
            .requesterHasOwnedProjectInResourceGisu(true)
            .superAdminAllowDraftRead(true)
            .build();
    }

    static ProjectAuthorizationEvaluationPoint internal() {
        return ProjectAuthorizationEvaluationPoint.internal(
            com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin
                .RESOURCE_PERMISSION_EVALUATOR
        );
    }

    static ProjectAuthorizationEvaluationPoint surface(String id) {
        return ProjectAuthorizationEvaluationPoint.surface(
            java.util.Arrays.stream(ProjectAuthorizationSurface.values())
                .filter(surface -> surface.id().equals(id))
                .findFirst()
                .orElseThrow()
        );
    }

    private static OrganizationType organizationType(ChallengerRoleType type) {
        if (type == ChallengerRoleType.CHAPTER_PRESIDENT) {
            return OrganizationType.CHAPTER;
        }
        if (type == ChallengerRoleType.SCHOOL_PRESIDENT
            || type == ChallengerRoleType.SCHOOL_VICE_PRESIDENT) {
            return OrganizationType.SCHOOL;
        }
        return OrganizationType.CENTRAL;
    }
}
