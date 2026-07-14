package com.umc.product.project.application.authorization.rollout;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

final class ProjectExpectedDifferenceFacts {

    private ProjectExpectedDifferenceFacts() {
    }

    static boolean supportsBoolean(String name) {
        return switch (name) {
            case "difference.expiredStaffForAction",
                "difference.expiredSuperAdminNonOwnerDecision",
                "difference.expiredSuperAdminOwnerInProgressDecision",
                "difference.expiredSuperAdminOwnerOutsideInProgressDecision",
                "difference.expiredStaffFullFormWithoutApplicant",
                "difference.expiredStaffFullFormWithApplicant",
                "difference.expiredPublicScopeStaff",
                "difference.expiredManagedCentralWithoutOwner",
                "difference.expiredManagedCentralWithOwner",
                "difference.expiredManagedChapterWithoutOwner",
                "difference.expiredManagedChapterWithOwner",
                "difference.expiredManagedSchoolWithoutOwner",
                "difference.expiredManagedSchoolWithOwner",
                "difference.expiredApplicationOngoingStaffWithoutBaseGrant",
                "difference.expiredApplicationOngoingStaffWithBaseGrant",
                "difference.expiredApplicationChapterStaffWithoutBaseGrant",
                "difference.expiredApplicationManagementSuperAdmin",
                "difference.expiredApplicationManagementCentral",
                "difference.expiredApplicationManagementChapter",
                "difference.activeChapterPresidentCrossChapter",
                "difference.legacyCreateHistoryWithoutTarget",
                "difference.activeChapterPresidentPublicScope",
                "difference.activeSuperAdminOtherGisu",
                "difference.activeSuperAdminOtherGisuWithoutOwner",
                "difference.activeSuperAdminOtherGisuWithOwner",
                "difference.activeSchoolCoreApplicationScope",
                "difference.activeStatisticsStaffOtherGisu",
                "difference.activeMatchingStaffOtherScope",
                "difference.activeSuperAdminDecisionCapability",
                "invariant.applicationRoundScopeMismatch",
                "synthetic.nullableSchedulerCaller" -> true;
            default -> false;
        };
    }

    static boolean booleanValue(String name, ProjectAuthorizationComparisonRequest request) {
        return switch (name) {
            case "difference.expiredStaffForAction" -> hasExpiredPrivilegedRole(request);
            case "difference.expiredSuperAdminNonOwnerDecision" -> expiredSuperAdmin(request) && !isOwner(request);
            case "difference.expiredSuperAdminOwnerInProgressDecision" ->
                expiredSuperAdmin(request) && isOwner(request) && projectInProgress(request);
            case "difference.expiredSuperAdminOwnerOutsideInProgressDecision" ->
                expiredSuperAdmin(request) && isOwner(request) && !projectInProgress(request);
            case "difference.expiredStaffFullFormWithoutApplicant" ->
                hasExpiredPrivilegedRole(request) && !isChallengerInResourceGisu(request);
            case "difference.expiredStaffFullFormWithApplicant" ->
                hasExpiredPrivilegedRole(request) && isChallengerInResourceGisu(request);
            case "difference.expiredPublicScopeStaff" -> expiredPublicScopeStaff(request);
            case "difference.expiredManagedCentralWithoutOwner" -> expiredManagedCentral(request) && !hasOwned(request);
            case "difference.expiredManagedCentralWithOwner" -> expiredManagedCentral(request) && hasOwned(request);
            case "difference.expiredManagedChapterWithoutOwner" -> expiredManagedChapter(request) && !hasOwned(request);
            case "difference.expiredManagedChapterWithOwner" -> expiredManagedChapter(request) && hasOwned(request);
            case "difference.expiredManagedSchoolWithoutOwner" -> expiredManagedSchool(request) && !hasOwned(request);
            case "difference.expiredManagedSchoolWithOwner" -> expiredManagedSchool(request) && hasOwned(request);
            case "difference.expiredApplicationOngoingStaffWithoutBaseGrant" ->
                expiredApplicationOngoingStaff(request) && !applicationBaseGrant(request);
            case "difference.expiredApplicationOngoingStaffWithBaseGrant" ->
                expiredApplicationOngoingStaff(request) && applicationBaseGrant(request);
            case "difference.expiredApplicationChapterStaffWithoutBaseGrant" ->
                expiredApplicationChapterStaff(request) && !applicationBaseGrant(request);
            case "difference.expiredApplicationManagementSuperAdmin" -> expiredSuperAdmin(request);
            case "difference.expiredApplicationManagementCentral" -> expiredManagedCentral(request);
            case "difference.expiredApplicationManagementChapter" -> expiredManagedChapter(request);
            case "difference.activeChapterPresidentCrossChapter" -> activeChapterPresidentCrossChapter(request);
            case "difference.legacyCreateHistoryWithoutTarget" -> legacyCreateHistoryWithoutTarget(request);
            case "difference.activeChapterPresidentPublicScope" -> activeChapterPresidentPublicScope(request);
            case "difference.activeSuperAdminOtherGisu",
                "difference.activeSuperAdminOtherGisuWithoutOwner" ->
                activeSuperAdminOtherGisu(request) && !hasOwned(request);
            case "difference.activeSuperAdminOtherGisuWithOwner" ->
                activeSuperAdminOtherGisu(request) && hasOwned(request);
            case "difference.activeSchoolCoreApplicationScope" -> activeSchoolCoreApplicationScope(request);
            case "difference.activeStatisticsStaffOtherGisu" -> activeStatisticsStaffOtherGisu(request);
            case "difference.activeMatchingStaffOtherScope" -> activeMatchingStaffOtherScope(request);
            case "difference.activeSuperAdminDecisionCapability" -> activeSuperAdminDecisionCapability(request);
            case "invariant.applicationRoundScopeMismatch" ->
                request.resourceSnapshot().applicationRoundScopeMismatch();
            case "synthetic.nullableSchedulerCaller" -> false;
            default -> throw new IllegalStateException("EXPECTED_DIFFERENCE_ATTRIBUTE_UNSUPPORTED");
        };
    }

    static Set<Long> longSet(String name, ProjectAuthorizationComparisonRequest request) {
        return switch (name) {
            case "resource.gisuId" -> Set.of(request.resource().gisuId().orElseThrow());
            case "resource.projectId" -> Set.of(request.resource().projectId().orElseThrow());
            case "subject.memberId" -> Set.of(memberId(request));
            case "difference.expiredChapterPresidentChapterIds" -> expiredChapterIds(request);
            case "difference.expiredSchoolCoreChapterIds" -> expiredSchoolChapterIds(request);
            case "relation.managedChapterIdsInResourceGisu" -> activeChapterIds(request);
            default -> throw new IllegalStateException("EXPECTED_DIFFERENCE_ATTRIBUTE_UNSUPPORTED");
        };
    }

    static boolean supportsLongSet(String name) {
        return switch (name) {
            case "resource.gisuId", "resource.projectId", "subject.memberId",
                "difference.expiredChapterPresidentChapterIds",
                "difference.expiredSchoolCoreChapterIds",
                "relation.managedChapterIdsInResourceGisu" -> true;
            default -> false;
        };
    }

    private static boolean hasExpiredPrivilegedRole(ProjectAuthorizationComparisonRequest request) {
        return request.snapshot().roles().stream()
            .anyMatch(role -> !role.isActiveAt(request.evaluatedAt()) && isPrivileged(role.roleType()));
    }

    private static boolean expiredSuperAdmin(ProjectAuthorizationComparisonRequest request) {
        return false;
    }

    private static boolean expiredPublicScopeStaff(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && !role.isActiveAt(request.evaluatedAt())
            && (role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT));
    }

    private static boolean expiredManagedCentral(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && !role.isActiveAt(request.evaluatedAt()) && role.roleType().isAtLeastCentralCore());
    }

    private static boolean expiredManagedChapter(ProjectAuthorizationComparisonRequest request) {
        return !expiredChapterIds(request).isEmpty() && !expiredManagedCentral(request);
    }

    private static boolean expiredManagedSchool(ProjectAuthorizationComparisonRequest request) {
        return !expiredSchoolChapterIds(request).isEmpty()
            && !expiredManagedCentral(request) && !expiredManagedChapter(request);
    }

    private static boolean expiredApplicationOngoingStaff(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        return request.snapshot().roles().stream().anyMatch(role -> !role.isActiveAt(request.evaluatedAt())
            && role.gisuId() == gisuId && role.roleType().isAtLeastCentralCore());
    }

    private static boolean expiredApplicationChapterStaff(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        long chapterId = resourceChapterId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && !role.isActiveAt(request.evaluatedAt())
            && (role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)
                || isSchoolCore(role) && Objects.equals(mappedChapter(request, role), chapterId)));
    }

    private static boolean activeChapterPresidentCrossChapter(ProjectAuthorizationComparisonRequest request) {
        if (!Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.ABORTED)
            .contains(request.resource().projectStatus().orElse(null))) {
            return false;
        }
        return request.snapshot().roles().stream().anyMatch(role -> role.isActiveAt(request.evaluatedAt())
            && role.gisuId() == resourceGisuId(request)
            && role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
            && !Objects.equals(role.organizationId(), resourceChapterId(request)));
    }

    private static boolean legacyCreateHistoryWithoutTarget(ProjectAuthorizationComparisonRequest request) {
        boolean legacyPlan = request.snapshot().challengers().stream()
            .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN);
        boolean targetPlan = request.snapshot().challengers().stream()
            .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN
                && challenger.gisuId() == resourceGisuId(request)
                && challenger.isActiveAt(request.evaluatedAt()));
        boolean legacyStaff = request.snapshot().roles().stream()
            .anyMatch(role -> isPrivileged(role.roleType()));
        boolean targetStaff = request.snapshot().superAdmin() || request.snapshot().roles().stream()
            .anyMatch(role -> activeCreateStaffForResource(request, role));
        return (legacyPlan || legacyStaff) && !targetPlan && !targetStaff && isCreator(request);
    }

    private static boolean activeChapterPresidentPublicScope(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        boolean chapter = request.snapshot().roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.isActiveAt(request.evaluatedAt())
            && role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
        boolean broader = request.snapshot().roles().stream().anyMatch(role -> role.isActiveAt(request.evaluatedAt())
            && role.gisuId() == gisuId && role.roleType().isAtLeastCentralCore());
        broader = broader || request.snapshot().superAdmin();
        return chapter && !broader;
    }

    private static boolean activeSuperAdminOtherGisu(ProjectAuthorizationComparisonRequest request) {
        return request.snapshot().superAdmin();
    }

    private static boolean activeSchoolCoreApplicationScope(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        long chapterId = resourceChapterId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() == gisuId
            && role.isActiveAt(request.evaluatedAt()) && isSchoolCore(role)
            && Objects.equals(mappedChapter(request, role), chapterId));
    }

    private static boolean activeStatisticsStaffOtherGisu(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        long chapterId = resourceChapterId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() != gisuId
            && role.isActiveAt(request.evaluatedAt())
            && (role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)
                || isSchoolCore(role)
                && request.resourceSnapshot().targetChapterSchoolIds().contains(role.organizationId())));
    }

    private static boolean activeMatchingStaffOtherScope(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        long chapterId = resourceChapterId(request);
        return request.snapshot().roles().stream().anyMatch(role -> role.gisuId() != gisuId
            && role.isActiveAt(request.evaluatedAt())
            && (role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)));
    }

    private static boolean activeSuperAdminDecisionCapability(ProjectAuthorizationComparisonRequest request) {
        return request.resource().applicationStatus().stream().anyMatch(status ->
            status == ProjectApplicationStatus.SUBMITTED || status == ProjectApplicationStatus.APPROVED
                || status == ProjectApplicationStatus.REJECTED)
            && !isOwner(request)
            && request.snapshot().superAdmin();
    }

    private static Set<Long> expiredChapterIds(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        Set<Long> ids = new LinkedHashSet<>();
        request.snapshot().roles().stream().filter(role -> role.gisuId() == gisuId)
            .filter(role -> !role.isActiveAt(request.evaluatedAt()))
            .filter(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(ProjectPolicyRoleTuple::organizationId).filter(Objects::nonNull).forEach(ids::add);
        return Set.copyOf(ids);
    }

    private static Set<Long> expiredSchoolChapterIds(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        Set<Long> ids = new LinkedHashSet<>();
        request.snapshot().roles().stream().filter(role -> role.gisuId() == gisuId)
            .filter(role -> !role.isActiveAt(request.evaluatedAt()) && isSchoolCore(role))
            .map(role -> mappedChapter(request, role)).filter(Objects::nonNull).forEach(ids::add);
        return Set.copyOf(ids);
    }

    private static Set<Long> activeChapterIds(ProjectAuthorizationComparisonRequest request) {
        long gisuId = resourceGisuId(request);
        Set<Long> ids = new LinkedHashSet<>();
        request.snapshot().roles().stream().filter(role -> role.gisuId() == gisuId)
            .filter(role -> role.isActiveAt(request.evaluatedAt()))
            .filter(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(ProjectPolicyRoleTuple::organizationId).filter(Objects::nonNull).forEach(ids::add);
        return Set.copyOf(ids);
    }

    private static boolean activeCreateStaffForResource(
        ProjectAuthorizationComparisonRequest request,
        ProjectPolicyRoleTuple role
    ) {
        if (!role.isActiveAt(request.evaluatedAt())) {
            return false;
        }
        if (role.gisuId() != resourceGisuId(request)) {
            return false;
        }
        return role.roleType().isAtLeastCentralCore()
            || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
            && Objects.equals(role.organizationId(), resourceChapterId(request))
            || isSchoolCore(role) && Objects.equals(mappedChapter(request, role), resourceChapterId(request));
    }

    private static boolean applicationBaseGrant(ProjectAuthorizationComparisonRequest request) {
        return isOwner(request) || request.resource().activePlanMember();
    }

    private static boolean isOwner(ProjectAuthorizationComparisonRequest request) {
        if (!(request.snapshot().principal() instanceof ProjectPolicyPrincipal.Member member)) {
            return false;
        }
        return request.resource().productOwnerMemberId().filter(id -> id == member.memberId()).isPresent();
    }

    private static boolean isCreator(ProjectAuthorizationComparisonRequest request) {
        if (!(request.snapshot().principal() instanceof ProjectPolicyPrincipal.Member member)) {
            return false;
        }
        return request.resource().creatorMemberId().filter(id -> id == member.memberId()).isPresent();
    }

    private static boolean isChallengerInResourceGisu(ProjectAuthorizationComparisonRequest request) {
        return request.snapshot().challengers().stream()
            .anyMatch(challenger -> challenger.gisuId() == resourceGisuId(request));
    }

    private static boolean hasOwned(ProjectAuthorizationComparisonRequest request) {
        return request.resource().requesterHasOwnedProjectInResourceGisu();
    }

    private static boolean projectInProgress(ProjectAuthorizationComparisonRequest request) {
        return request.resource().projectStatus().filter(status -> status == ProjectStatus.IN_PROGRESS).isPresent();
    }

    private static long memberId(ProjectAuthorizationComparisonRequest request) {
        return ((ProjectPolicyPrincipal.Member) request.snapshot().principal()).memberId();
    }

    private static long resourceGisuId(ProjectAuthorizationComparisonRequest request) {
        return request.resource().gisuId().orElseThrow();
    }

    private static long resourceChapterId(ProjectAuthorizationComparisonRequest request) {
        return request.resource().chapterId().orElseThrow();
    }

    private static Long mappedChapter(
        ProjectAuthorizationComparisonRequest request,
        ProjectPolicyRoleTuple role
    ) {
        if (role.organizationId() == null) {
            return null;
        }
        return request.snapshot().chapterIdByGisuAndSchool()
            .get(new ProjectPolicySchoolChapterKey(role.gisuId(), role.organizationId()));
    }

    private static boolean isSchoolCore(ProjectPolicyRoleTuple role) {
        return role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
            || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }

    private static boolean isPrivileged(ChallengerRoleType roleType) {
        return roleType.isAtLeastCentralCore()
            || roleType == ChallengerRoleType.CHAPTER_PRESIDENT || roleType.isAtLeastSchoolCore();
    }
}
