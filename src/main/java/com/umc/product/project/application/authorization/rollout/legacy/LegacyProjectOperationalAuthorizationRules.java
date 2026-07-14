package com.umc.product.project.application.authorization.rollout.legacy;

import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;

final class LegacyProjectOperationalAuthorizationRules {

    boolean canReadProjectStatistics(
        ProjectPolicySubjectSnapshot subject,
        ProjectAuthorizationResourceSnapshot resourceSnapshot
    ) {
        ProjectPolicyResourceContext resource = resourceSnapshot.policyContext();
        Long memberId = memberId(subject);
        boolean owner = memberId != null
            && resource.productOwnerMemberId().filter(memberId::equals).isPresent();
        return owner || resource.activePlanMember() || canReadChapterStatistics(subject, resourceSnapshot);
    }

    boolean canReadChapterStatistics(
        ProjectPolicySubjectSnapshot subject,
        ProjectAuthorizationResourceSnapshot resourceSnapshot
    ) {
        ProjectPolicyResourceContext resource = resourceSnapshot.policyContext();
        long chapterId = resource.chapterId().orElseThrow();
        if (subject.roles().stream().anyMatch(role -> role.roleType().isAtLeastCentralCore()
            || (role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)))) {
            return true;
        }

        return subject.roles().stream()
            .filter(this::isSchoolCore)
            .map(ProjectPolicyRoleTuple::organizationId)
            .filter(Objects::nonNull)
            .anyMatch(resourceSnapshot.targetChapterSchoolIds()::contains);
    }

    boolean canManageMatchingRound(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        if (!(subject.principal() instanceof ProjectPolicyPrincipal.Member)) {
            return false;
        }
        long chapterId = resource.chapterId().orElseThrow();
        return subject.roles().stream().anyMatch(role -> role.roleType().isAtLeastCentralCore()
            || (role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)));
    }

    boolean canRunScheduler(ProjectPolicySubjectSnapshot subject) {
        return subject.principal() instanceof ProjectPolicyPrincipal.SystemPrincipal;
    }

    private boolean isSchoolCore(ProjectPolicyRoleTuple role) {
        return role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
            || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }

    private Long memberId(ProjectPolicySubjectSnapshot subject) {
        if (subject.principal() instanceof ProjectPolicyPrincipal.Member member) {
            return member.memberId();
        }
        return null;
    }
}
