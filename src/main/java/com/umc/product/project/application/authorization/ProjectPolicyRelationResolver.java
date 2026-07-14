package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public class ProjectPolicyRelationResolver {

    public ProjectPolicyRelationFacts resolve(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return resolve(subject, resource, Optional.empty());
    }

    public ProjectPolicyRelationFacts resolve(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource,
        Optional<Long> targetMemberSchoolId
    ) {
        Instant evaluatedAt = subject.evaluatedAt();
        Long resourceGisuId = resource.gisuId().orElse(null);
        Long resourceChapterId = resource.chapterId().orElse(null);

        boolean activeSuperAdmin = subject.superAdmin();
        boolean activeCentral = subject.roles().stream()
            .anyMatch(role -> isCentralCore(role)
                && Objects.equals(role.gisuId(), resourceGisuId)
                && role.isActiveAt(evaluatedAt));
        boolean activeChapter = subject.roles().stream()
            .anyMatch(role -> isChapterPresident(role)
                && Objects.equals(role.gisuId(), resourceGisuId)
                && Objects.equals(role.organizationId(), resourceChapterId)
                && role.isActiveAt(evaluatedAt));
        boolean activeSchool = subject.roles().stream()
            .anyMatch(role -> isSchoolCore(role)
                && Objects.equals(role.gisuId(), resourceGisuId)
                && matchesSchoolResource(subject, role, resourceChapterId, targetMemberSchoolId)
                && role.isActiveAt(evaluatedAt));

        Set<Long> managedGisuIds = new TreeSet<>();
        Set<Long> managedChapterIds = new TreeSet<>();
        Set<Long> managedSchoolChapterIds = new TreeSet<>();
        for (ProjectPolicyRoleTuple role : subject.roles()) {
            if (!role.isActiveAt(evaluatedAt) || !Objects.equals(role.gisuId(), resourceGisuId)) {
                continue;
            }
            if (isCentralCore(role)) {
                managedGisuIds.add(role.gisuId());
            }
            if (isChapterPresident(role) && role.organizationId() != null) {
                managedChapterIds.add(role.organizationId());
            }
            if (isSchoolCore(role)) {
                Long mapped = mappedChapterId(subject, role);
                if (mapped != null) {
                    managedSchoolChapterIds.add(mapped);
                }
            }
        }

        boolean activePlanChallenger = subject.challengers().stream()
            .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN
                && Objects.equals(challenger.gisuId(), resourceGisuId)
                && challenger.isActiveAt(evaluatedAt));
        boolean challengerInGisu = subject.challengers().stream()
            .anyMatch(challenger -> Objects.equals(challenger.gisuId(), resourceGisuId));

        Set<Long> requesterMemberIds = memberId(subject).map(Set::of).orElseGet(Set::of);
        Set<Long> resourceProjectIds = resource.projectId().map(Set::of).orElseGet(Set::of);
        Long memberId = memberId(subject).orElse(null);

        return new ProjectPolicyRelationFacts(
            activeSuperAdmin,
            activeCentral,
            activeChapter,
            activeSchool,
            activePlanChallenger,
            !managedChapterIds.isEmpty(),
            !managedSchoolChapterIds.isEmpty(),
            memberId != null && resource.creatorMemberId().filter(memberId::equals).isPresent(),
            memberId != null && resource.productOwnerMemberId().filter(memberId::equals).isPresent(),
            memberId != null && resource.applicantMemberId().filter(memberId::equals).isPresent(),
            memberId != null && resource.activePlanMember(),
            challengerInGisu,
            memberId != null && resource.requesterHasOwnedProjectInResourceGisu(),
            managedGisuIds,
            managedChapterIds,
            managedSchoolChapterIds,
            requesterMemberIds,
            resourceProjectIds
        );
    }

    private boolean matchesSchoolResource(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyRoleTuple role,
        Long resourceChapterId,
        Optional<Long> targetMemberSchoolId
    ) {
        return targetMemberSchoolId
            .map(targetSchoolId -> Objects.equals(role.organizationId(), targetSchoolId))
            .orElseGet(() -> Objects.equals(mappedChapterId(subject, role), resourceChapterId));
    }

    private java.util.Optional<Long> memberId(ProjectPolicySubjectSnapshot subject) {
        if (subject.principal() instanceof ProjectPolicyPrincipal.Member member) {
            return java.util.Optional.of(member.memberId());
        }
        return java.util.Optional.empty();
    }

    private boolean isCentralCore(ProjectPolicyRoleTuple role) {
        return role.organizationType() == OrganizationType.CENTRAL
            && (role.roleType() == ChallengerRoleType.CENTRAL_PRESIDENT
                || role.roleType() == ChallengerRoleType.CENTRAL_VICE_PRESIDENT);
    }

    private boolean isChapterPresident(ProjectPolicyRoleTuple role) {
        return role.organizationType() == OrganizationType.CHAPTER
            && role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT;
    }

    private boolean isSchoolCore(ProjectPolicyRoleTuple role) {
        return role.organizationType() == OrganizationType.SCHOOL
            && (role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT);
    }

    private Long mappedChapterId(ProjectPolicySubjectSnapshot subject, ProjectPolicyRoleTuple role) {
        if (role.organizationId() == null) {
            return null;
        }
        return subject.chapterIdByGisuAndSchool()
            .get(new ProjectPolicySchoolChapterKey(role.gisuId(), role.organizationId()));
    }
}
