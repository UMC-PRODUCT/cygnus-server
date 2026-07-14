package com.umc.product.project.application.authorization.rollout.legacy;

import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

final class LegacyProjectAuthorizationRules {

    boolean canReadProject(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        ProjectStatus status = resource.projectStatus().orElseThrow();
        return switch (status) {
            case IN_PROGRESS, COMPLETED -> true;
            case DRAFT -> isProductOwner(subject, resource)
                || (resource.superAdminAllowDraftRead() && isSuperAdmin(subject));
            case PENDING_REVIEW, ABORTED -> isProductOwner(subject, resource)
                || isCentralCoreInGisu(subject, resource.gisuId().orElseThrow())
                || isChapterPresidentInGisu(subject, resource.gisuId().orElseThrow());
        };
    }

    boolean canEnterProjectCreate(ProjectPolicySubjectSnapshot subject) {
        boolean planChallenger = subject.challengers().stream()
            .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN);
        return planChallenger || isSuperAdmin(subject) || subject.roles().stream()
            .map(ProjectPolicyRoleTuple::roleType)
            .anyMatch(this::isProjectCreateStaff);
    }

    boolean canAssignProjectOwner(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource,
        long targetMemberSchoolId
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        long chapterId = resource.chapterId().orElseThrow();
        return subject.roles().stream()
            .filter(role -> role.gisuId() == gisuId)
            .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || isChapterPresidentOf(role, chapterId)
                || isSchoolCoreOf(role, targetMemberSchoolId));
    }

    boolean canReadApplication(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        if (isApplicant(subject, resource)) {
            return true;
        }
        ProjectApplicationStatus status = resource.applicationStatus().orElseThrow();
        if (status == ProjectApplicationStatus.DRAFT) {
            return resource.superAdminAllowDraftRead() && isSuperAdmin(subject);
        }
        if (isProductOwner(subject, resource) || resource.activePlanMember()) {
            return true;
        }
        long gisuId = resource.gisuId().orElseThrow();
        long chapterId = resource.chapterId().orElseThrow();
        return isCentralCoreInGisu(subject, gisuId)
            || subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
                && isChapterPresidentOf(role, chapterId));
    }

    boolean canEditProject(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return switch (resource.projectStatus().orElseThrow()) {
            case DRAFT -> isCreator(subject, resource);
            case PENDING_REVIEW, IN_PROGRESS -> isProductOwner(subject, resource)
                || isProjectAdmin(subject, resource);
            case COMPLETED, ABORTED -> false;
        };
    }

    boolean canManageProject(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return switch (resource.projectStatus().orElseThrow()) {
            case PENDING_REVIEW, IN_PROGRESS -> isProjectAdmin(subject, resource);
            case DRAFT, COMPLETED, ABORTED -> false;
        };
    }

    boolean canDeleteProject(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return switch (resource.projectStatus().orElseThrow()) {
            case DRAFT, PENDING_REVIEW -> isProductOwner(subject, resource)
                || isProjectAdmin(subject, resource);
            case IN_PROGRESS, COMPLETED, ABORTED -> false;
        };
    }

    boolean canCreateApplication(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        return subject.challengers().stream()
            .anyMatch(challenger -> challenger.gisuId() == gisuId);
    }

    boolean canEditApplication(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return isApplicant(subject, resource)
            && resource.applicationStatus().orElseThrow() == ProjectApplicationStatus.DRAFT;
    }

    boolean canCancelApplication(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        if (!isApplicant(subject, resource)) {
            return false;
        }
        return switch (resource.applicationStatus().orElseThrow()) {
            case DRAFT, SUBMITTED -> true;
            case APPROVED, REJECTED, CANCELLED -> false;
        };
    }

    boolean canDecideApplication(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        boolean decidable = switch (resource.applicationStatus().orElseThrow()) {
            case SUBMITTED, APPROVED, REJECTED -> true;
            case DRAFT, CANCELLED -> false;
        };
        return decidable && (isProductOwner(subject, resource) || isSuperAdmin(subject));
    }

    boolean canViewApplicantForm(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        return subject.challengers().stream()
            .anyMatch(challenger -> challenger.gisuId() == gisuId);
    }

    boolean canViewFullForm(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource,
        boolean batchEvaluation
    ) {
        if (isProductOwner(subject, resource)) {
            return true;
        }
        long gisuId = resource.gisuId().orElseThrow();
        if (batchEvaluation && isSuperAdmin(subject)) {
            return true;
        }
        if (subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.roleType().isAtLeastCentralCore())) {
            return true;
        }
        long chapterId = resource.chapterId().orElseThrow();
        return subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && isChapterPresidentOf(role, chapterId));
    }

    private boolean isProjectCreateStaff(ChallengerRoleType roleType) {
        return roleType.isAtLeastCentralCore()
            || roleType == ChallengerRoleType.CHAPTER_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }

    boolean isCentralCoreInGisu(ProjectPolicySubjectSnapshot subject, long gisuId) {
        return isSuperAdmin(subject) || subject.roles().stream()
            .anyMatch(role -> role.gisuId() == gisuId && role.roleType().isAtLeastCentralCore());
    }

    private boolean isChapterPresidentInGisu(ProjectPolicySubjectSnapshot subject, long gisuId) {
        return subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
    }

    boolean isSuperAdmin(ProjectPolicySubjectSnapshot subject) {
        return subject.superAdmin();
    }

    private boolean isProductOwner(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return memberId(subject) != null
            && resource.productOwnerMemberId().filter(memberId(subject)::equals).isPresent();
    }

    private boolean isApplicant(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return memberId(subject) != null
            && resource.applicantMemberId().filter(memberId(subject)::equals).isPresent();
    }

    Long memberId(ProjectPolicySubjectSnapshot subject) {
        if (subject.principal() instanceof ProjectPolicyPrincipal.Member member) {
            return member.memberId();
        }
        return null;
    }

    private boolean isChapterPresidentOf(ProjectPolicyRoleTuple role, long chapterId) {
        return role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
            && Objects.equals(role.organizationId(), chapterId);
    }

    private boolean isSchoolCoreOf(ProjectPolicyRoleTuple role, long schoolId) {
        return (role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
            || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            && Objects.equals(role.organizationId(), schoolId);
    }

    private boolean isCreator(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        Long memberId = memberId(subject);
        return memberId != null && resource.creatorMemberId().filter(memberId::equals).isPresent();
    }

    private boolean isProjectAdmin(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        long gisuId = resource.gisuId().orElseThrow();
        long chapterId = resource.chapterId().orElseThrow();
        return isCentralCoreInGisu(subject, gisuId)
            || subject.roles().stream().anyMatch(role -> role.gisuId() == gisuId
                && isChapterPresidentOf(role, chapterId));
    }
}
