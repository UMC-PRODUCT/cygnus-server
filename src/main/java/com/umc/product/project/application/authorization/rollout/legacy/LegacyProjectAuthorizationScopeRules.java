package com.umc.product.project.application.authorization.rollout.legacy;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationApplicationScope;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationProjectScope;

final class LegacyProjectAuthorizationScopeRules {

    ProjectAuthorizationProjectScope publicProjects(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        boolean staff = subject.roles().stream()
            .filter(role -> role.gisuId() == gisuId)
            .map(ProjectPolicyRoleTuple::roleType)
            .anyMatch(role -> role.isAtLeastCentralCore()
                || role == ChallengerRoleType.CHAPTER_PRESIDENT);
        return staff
            ? new ProjectAuthorizationProjectScope(
                false, false, Set.of(gisuId), Set.of(), Set.of(), false)
            : new ProjectAuthorizationProjectScope(
                false, true, Set.of(), Set.of(), Set.of(), false);
    }

    ProjectAuthorizationProjectScope managedProjects(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        Set<Long> gisuIds = Set.of();
        Set<Long> chapterIds = Set.of();
        if (hasCentralCoreInGisu(subject, gisuId)) {
            gisuIds = Set.of(gisuId);
        } else {
            Long chapterId = firstOrganizationId(subject, gisuId, ChallengerRoleType.CHAPTER_PRESIDENT);
            if (chapterId != null) {
                chapterIds = Set.of(chapterId);
            } else {
                Long schoolId = firstSchoolCoreOrganizationId(subject, gisuId);
                Long mappedChapterId = mappedChapterId(subject, gisuId, schoolId);
                if (mappedChapterId != null) {
                    chapterIds = Set.of(mappedChapterId);
                }
            }
        }

        Set<Long> ownerMemberIds = resource.requesterHasOwnedProjectInResourceGisu()
            ? memberId(subject).map(Set::of).orElseGet(Set::of)
            : Set.of();
        return new ProjectAuthorizationProjectScope(
            false,
            false,
            gisuIds,
            chapterIds,
            ownerMemberIds,
            !ownerMemberIds.isEmpty()
        );
    }

    ProjectAuthorizationProjectScope ownDrafts(ProjectPolicySubjectSnapshot subject) {
        Set<Long> ownerMemberIds = memberId(subject).map(Set::of).orElseGet(Set::of);
        return new ProjectAuthorizationProjectScope(
            false, false, Set.of(), Set.of(), ownerMemberIds, !ownerMemberIds.isEmpty());
    }

    ProjectAuthorizationApplicationScope ownApplications(ProjectPolicySubjectSnapshot subject) {
        Set<Long> ownerMemberIds = memberId(subject).map(Set::of).orElseGet(Set::of);
        return new ProjectAuthorizationApplicationScope(
            false, Set.of(), Set.of(), Set.of(), ownerMemberIds, false);
    }

    ProjectAuthorizationApplicationScope projectApplications(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long projectId = resource.projectId().orElseThrow();
        long gisuId = resource.gisuId().orElseThrow();
        long chapterId = resource.chapterId().orElseThrow();
        Long memberId = memberId(subject).orElse(null);
        boolean owner = memberId != null
            && resource.productOwnerMemberId().filter(memberId::equals).isPresent();
        boolean superAdmin = hasSuperAdmin(subject);
        boolean centralCore = hasCentralCoreInGisu(subject, gisuId);
        boolean chapterPresident = subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
            && Objects.equals(role.organizationId(), chapterId));
        boolean schoolCore = subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && isSchoolCore(role)
            && Objects.equals(mappedChapterId(subject, gisuId, role.organizationId()), chapterId));
        boolean allowed = owner || resource.activePlanMember() || superAdmin || centralCore
            || chapterPresident || schoolCore;
        return allowed
            ? new ProjectAuthorizationApplicationScope(
                false, Set.of(), Set.of(), Set.of(projectId), Set.of(), superAdmin || centralCore)
            : ProjectAuthorizationApplicationScope.none();
    }

    ProjectAuthorizationApplicationScope managedApplications(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        if (hasSuperAdmin(subject)) {
            return new ProjectAuthorizationApplicationScope(
                true, Set.of(), Set.of(), Set.of(), Set.of(), false);
        }
        if (hasCentralCoreInGisu(subject, gisuId)) {
            return new ProjectAuthorizationApplicationScope(
                false, Set.of(gisuId), Set.of(), Set.of(), Set.of(), false);
        }
        Set<Long> chapterIds = new LinkedHashSet<>();
        subject.roles().stream()
            .filter(role -> role.gisuId() == gisuId)
            .filter(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(ProjectPolicyRoleTuple::organizationId)
            .filter(Objects::nonNull)
            .forEach(chapterIds::add);
        return new ProjectAuthorizationApplicationScope(
            false, Set.of(), chapterIds, Set.of(), Set.of(), false);
    }

    private boolean hasSuperAdmin(ProjectPolicySubjectSnapshot subject) {
        return subject.superAdmin();
    }

    private boolean hasCentralCoreInGisu(ProjectPolicySubjectSnapshot subject, long gisuId) {
        return subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.roleType().isAtLeastCentralCore());
    }

    private Long firstOrganizationId(
        ProjectPolicySubjectSnapshot subject,
        long gisuId,
        ChallengerRoleType roleType
    ) {
        return subject.roles().stream()
            .filter(role -> role.gisuId() == gisuId)
            .filter(role -> role.roleType() == roleType)
            .map(ProjectPolicyRoleTuple::organizationId)
            .findFirst()
            .orElse(null);
    }

    private Long firstSchoolCoreOrganizationId(ProjectPolicySubjectSnapshot subject, long gisuId) {
        return subject.roles().stream()
            .filter(role -> role.gisuId() == gisuId)
            .filter(this::isSchoolCore)
            .map(ProjectPolicyRoleTuple::organizationId)
            .findFirst()
            .orElse(null);
    }

    private Long mappedChapterId(
        ProjectPolicySubjectSnapshot subject,
        long gisuId,
        Long schoolId
    ) {
        return schoolId == null
            ? null
            : subject.chapterIdByGisuAndSchool().get(new ProjectPolicySchoolChapterKey(gisuId, schoolId));
    }

    private boolean isSchoolCore(ProjectPolicyRoleTuple role) {
        return role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
            || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }

    private java.util.Optional<Long> memberId(ProjectPolicySubjectSnapshot subject) {
        if (subject.principal()
            instanceof com.umc.product.project.application.authorization.ProjectPolicyPrincipal.Member member) {
            return java.util.Optional.of(member.memberId());
        }
        return java.util.Optional.empty();
    }
}
