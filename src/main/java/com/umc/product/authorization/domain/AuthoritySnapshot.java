package com.umc.product.authorization.domain;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 권한 판단에 필요한 subject 상태를 모은 snapshot입니다.
 */
public record AuthoritySnapshot(
    Long memberId,
    Long schoolId,
    List<SubjectAttributes.GisuChallengerInfo> gisuChallengerInfos,
    List<RoleAttribute> challengerRoles,
    Set<SystemRoleType> systemRoles
) {

    public AuthoritySnapshot {
        gisuChallengerInfos = gisuChallengerInfos == null ? List.of() : List.copyOf(gisuChallengerInfos);
        challengerRoles = challengerRoles == null ? List.of() : List.copyOf(challengerRoles);
        systemRoles = systemRoles == null ? Set.of() : Set.copyOf(systemRoles);
    }

    public static AuthoritySnapshot of(
        Long memberId,
        Long schoolId,
        List<SubjectAttributes.GisuChallengerInfo> gisuChallengerInfos,
        List<RoleAttribute> challengerRoles,
        Set<SystemRoleType> systemRoles
    ) {
        return new AuthoritySnapshot(memberId, schoolId, gisuChallengerInfos, challengerRoles, systemRoles);
    }

    public static AuthoritySnapshot from(SubjectAttributes subjectAttributes) {
        return AuthoritySnapshot.of(
            subjectAttributes.memberId(),
            subjectAttributes.schoolId(),
            subjectAttributes.gisuChallengerInfos(),
            subjectAttributes.roleAttributes(),
            subjectAttributes.systemRoles()
        );
    }

    public SubjectAttributes toSubjectAttributes() {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .gisuChallengerInfos(gisuChallengerInfos)
            .roleAttributes(challengerRoles)
            .systemRoles(systemRoles)
            .build();
    }

    public boolean isSuperAdmin() {
        return systemRoles.contains(SystemRoleType.SUPER_ADMIN)
            || challengerRoles.stream()
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isSuperAdmin);
    }

    public boolean isCentralCoreInAnyGisu() {
        return isSuperAdmin() || challengerRoles.stream()
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    public boolean isCentralCoreInGisu(Long gisuId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    public boolean isCentralMemberInAnyGisu() {
        return isSuperAdmin() || challengerRoles.stream()
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralMember);
    }

    public boolean isCentralMemberInGisu(Long gisuId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralMember);
    }

    public boolean isSchoolCoreInAnyGisu(Long schoolId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), schoolId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolCore);
    }

    public boolean isSchoolCoreInGisu(Long gisuId, Long schoolId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), schoolId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolCore);
    }

    public boolean isSchoolAdminInAnyGisu(Long schoolId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), schoolId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolAdmin);
    }

    public boolean isSchoolAdminInGisu(Long gisuId, Long schoolId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), schoolId))
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolAdmin);
    }

    public boolean isChapterPresidentInAnyGisu(Long chapterId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> role.organizationType() == OrganizationType.CHAPTER)
            .filter(role -> Objects.equals(role.organizationId(), chapterId))
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
    }

    public boolean isChapterPresidentInGisu(Long gisuId, Long chapterId) {
        return isSuperAdmin() || challengerRoles.stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .filter(role -> role.organizationType() == OrganizationType.CHAPTER)
            .filter(role -> Objects.equals(role.organizationId(), chapterId))
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
    }
}
