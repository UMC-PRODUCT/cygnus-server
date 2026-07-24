package com.umc.product.notice.application.authorization;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.AuthorizationChallengerTuple;
import com.umc.product.authorization.domain.AuthorizationRoleTuple;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.enums.NoticeTargetPattern;

@Component
public class NoticePolicyRelationResolver {

    NoticePolicyRelations resolve(NoticeAuthorizationContext context) {
        AuthorizationSubjectSnapshot subject = context.subject().orElse(null);
        if (subject == null) {
            return new NoticePolicyRelations(false, false, false, false,
                context.author(), false, false);
        }
        boolean superAdmin = subject.isSuperAdmin();
        List<AuthorizationRoleTuple> activeRoles = subject.roles().stream()
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean activeCentralCore = superAdmin || activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean targetChallenger = isTargetChallenger(subject, context.target());
        boolean roleCanRead = activeRoles.stream()
            .anyMatch(role -> canReadByRole(role, context.target(), subject));
        boolean manager = superAdmin || activeManager(activeRoles, context.target());
        boolean creator = context.action() == NoticePolicyAction.CREATE
            && (superAdmin || activeCreator(activeRoles, context.target(), context.targetPattern()));
        return new NoticePolicyRelations(
            superAdmin,
            activeCentralCore,
            targetChallenger,
            roleCanRead,
            context.author(),
            manager,
            creator);
    }

    private boolean isTargetChallenger(
        AuthorizationSubjectSnapshot subject,
        NoticeTargetInfo target
    ) {
        if (target.isStaffNotice()) {
            return false;
        }
        Long schoolId = subject.schoolId().isPresent() ? subject.schoolId().getAsLong() : null;
        return subject.challengers().stream().anyMatch(challenger ->
            target.isTarget(
                challenger.gisuId(),
                challenger.chapterId(),
                schoolId,
                challenger.part()));
    }

    private boolean canReadByRole(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target,
        AuthorizationSubjectSnapshot subject
    ) {
        if (target.isStaffNotice()) {
            return canReadStaffNotice(role, target);
        }
        return switch (role.roleType()) {
            case CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER ->
                target.targetGisuId() == null || role.gisuId() == target.targetGisuId();
            case CHAPTER_PRESIDENT -> chapterPresidentCanRead(role, target, subject);
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT ->
                schoolCoreCanRead(role, target, subject);
            case SCHOOL_PART_LEADER -> schoolPartLeaderCanRead(role, target, subject);
            default -> false;
        };
    }

    private boolean canReadStaffNotice(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target
    ) {
        NoticeTab viewerRole = NoticeTab.findFrom(role.roleType()).orElse(null);
        if (viewerRole == null || !target.targetNoticeTab().includes(viewerRole)) {
            return false;
        }
        if (target.targetGisuId() != null && role.gisuId() != target.targetGisuId()) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !Objects.equals(target.targetSchoolId(), role.organizationId())) {
            return false;
        }
        if (viewerRole == NoticeTab.SCHOOL_PART_LEADER
            && target.targetParts() != null
            && !target.targetParts().isEmpty()) {
            boolean partFree = role.responsiblePart() == null
                || role.responsiblePart() == ChallengerPart.ADMIN;
            return partFree || target.targetParts().contains(role.responsiblePart());
        }
        return true;
    }

    private boolean chapterPresidentCanRead(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target,
        AuthorizationSubjectSnapshot subject
    ) {
        Long chapterId = role.organizationId();
        if (chapterId == null) {
            return false;
        }
        if (target.targetChapterId() != null
            && !chapterId.equals(target.targetChapterId())) {
            return false;
        }
        Long schoolId = subject.schoolId().isPresent() ? subject.schoolId().getAsLong() : null;
        if (target.targetSchoolId() != null
            && !Objects.equals(schoolId, target.targetSchoolId())) {
            return false;
        }
        if (target.targetChapterId() == null && target.targetSchoolId() == null) {
            return false;
        }
        return subject.challengers().stream()
            .filter(challenger -> challenger.chapterId() == chapterId)
            .filter(challenger -> challenger.gisuId() == role.gisuId())
            .anyMatch(challenger -> target.targetGisuId() == null
                || challenger.gisuId() == target.targetGisuId());
    }

    private boolean schoolCoreCanRead(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target,
        AuthorizationSubjectSnapshot subject
    ) {
        if (role.organizationId() == null) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !Objects.equals(target.targetSchoolId(), role.organizationId())) {
            return false;
        }
        return isInGisuAndChapter(role, target, subject.challengers());
    }

    private boolean schoolPartLeaderCanRead(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target,
        AuthorizationSubjectSnapshot subject
    ) {
        if (role.responsiblePart() == null || role.organizationId() == null) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !Objects.equals(target.targetSchoolId(), role.organizationId())) {
            return false;
        }
        if (target.targetParts() == null
            || target.targetParts().isEmpty()
            || !target.targetParts().contains(role.responsiblePart())) {
            return false;
        }
        return isInGisuAndChapter(role, target, subject.challengers());
    }

    private boolean isInGisuAndChapter(
        AuthorizationRoleTuple role,
        NoticeTargetInfo target,
        List<AuthorizationChallengerTuple> challengers
    ) {
        return challengers.stream()
            .filter(challenger -> challenger.gisuId() == role.gisuId())
            .anyMatch(challenger ->
                (target.targetGisuId() == null || challenger.gisuId() == target.targetGisuId())
                    && (target.targetChapterId() == null
                        || challenger.chapterId() == target.targetChapterId()));
    }

    private boolean activeManager(
        List<AuthorizationRoleTuple> roles,
        NoticeTargetInfo target
    ) {
        if (roles.stream().anyMatch(role -> role.roleType().isAtLeastCentralCore())) {
            return true;
        }
        if (target.targetSchoolId() != null) {
            return roles.stream()
                .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
                .filter(role -> Objects.equals(role.organizationId(), target.targetSchoolId()))
                .anyMatch(role -> role.roleType().isAtLeastSchoolAdmin());
        }
        if (target.targetChapterId() != null) {
            return roles.stream()
                .filter(role -> role.organizationType() == OrganizationType.CHAPTER)
                .filter(role -> Objects.equals(role.organizationId(), target.targetChapterId()))
                .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
        }
        return roles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralMember());
    }

    private boolean activeCreator(
        List<AuthorizationRoleTuple> roles,
        NoticeTargetInfo target,
        NoticeTargetPattern pattern
    ) {
        return switch (pattern) {
            case ALL_GISU_ALL_TARGET -> roles.stream()
                .anyMatch(role -> role.roleType().isAtLeastCentralCore());
            case ALL_GISU_SPECIFIC_SCHOOL -> schoolRole(
                roles,
                target,
                ChallengerRoleType::isAtLeastSchoolCore,
                false);
            case SPECIFIC_GISU_ALL_TARGET, SPECIFIC_GISU_SPECIFIC_PART ->
                centralRoleInTargetGisu(roles, target, false);
            case SPECIFIC_GISU_SPECIFIC_SCHOOL -> schoolRole(
                roles,
                target,
                ChallengerRoleType::isAtLeastSchoolCore,
                true);
            case SPECIFIC_GISU_SPECIFIC_SCHOOL_WITH_PART -> schoolRole(
                roles,
                target,
                ChallengerRoleType::isAtLeastSchoolAdmin,
                true);
            case SPECIFIC_GISU_SPECIFIC_CHAPTER,
                SPECIFIC_GISU_SPECIFIC_CHAPTER_WITH_PART -> roles.stream()
                    .filter(role -> role.gisuId() == target.targetGisuId())
                    .filter(role -> role.organizationType() == OrganizationType.CHAPTER)
                    .filter(role -> Objects.equals(role.organizationId(), target.targetChapterId()))
                    .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
            case STAFF_SPECIFIC_GISU,
                STAFF_SPECIFIC_GISU_SPECIFIC_PART,
                STAFF_SPECIFIC_GISU_SPECIFIC_SCHOOL -> staffCreator(roles, target);
            case ALL_GISU_SPECIFIC_SCHOOL_WITH_PART,
                ALL_GISU_WITH_CHAPTER,
                INVALID_GISU_CHAPTER_SCHOOL -> false;
        };
    }

    private boolean staffCreator(
        List<AuthorizationRoleTuple> roles,
        NoticeTargetInfo target
    ) {
        if (target.targetNoticeTab() == NoticeTab.CENTRAL_MEMBER) {
            return roles.stream()
                .filter(role -> role.gisuId() == target.targetGisuId())
                .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        }
        if (target.targetSchoolId() != null) {
            return schoolRole(
                roles,
                target,
                ChallengerRoleType::isAtLeastSchoolCore,
                true);
        }
        return centralRoleInTargetGisu(roles, target, false);
    }

    private boolean centralRoleInTargetGisu(
        List<AuthorizationRoleTuple> roles,
        NoticeTargetInfo target,
        boolean coreOnly
    ) {
        return roles.stream()
            .filter(role -> role.gisuId() == target.targetGisuId())
            .anyMatch(role -> coreOnly
                ? role.roleType().isAtLeastCentralCore()
                : role.roleType().isAtLeastCentralMember());
    }

    private boolean schoolRole(
        List<AuthorizationRoleTuple> roles,
        NoticeTargetInfo target,
        java.util.function.Predicate<ChallengerRoleType> rolePredicate,
        boolean requireTargetGisu
    ) {
        return roles.stream()
            .filter(role -> !requireTargetGisu || role.gisuId() == target.targetGisuId())
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), target.targetSchoolId()))
            .anyMatch(role -> rolePredicate.test(role.roleType()));
    }
}
