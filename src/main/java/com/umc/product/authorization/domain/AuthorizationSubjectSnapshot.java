package com.umc.product.authorization.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

public record AuthorizationSubjectSnapshot(
    AuthorizationPrincipal principal,
    OptionalLong schoolId,
    Instant evaluatedAt,
    Set<SystemRoleType> systemRoles,
    List<AuthorizationRoleTuple> roles,
    List<AuthorizationChallengerTuple> challengers,
    Map<AuthorizationSchoolChapterKey, Long> chapterIdByGisuAndSchool
) {
    public AuthorizationSubjectSnapshot {
        Objects.requireNonNull(principal);
        Objects.requireNonNull(schoolId);
        Objects.requireNonNull(evaluatedAt);
        systemRoles = Set.copyOf(systemRoles);
        roles = List.copyOf(roles);
        challengers = List.copyOf(challengers);
        chapterIdByGisuAndSchool = Map.copyOf(chapterIdByGisuAndSchool);
        schoolId.ifPresent(value -> {
            if (value <= 0) {
                throw new IllegalArgumentException("schoolId는 양수여야 합니다.");
            }
        });
        validatePrincipalScope(
            principal,
            schoolId,
            systemRoles,
            roles,
            challengers,
            chapterIdByGisuAndSchool);
    }

    public static AuthorizationSubjectSnapshot member(
        long memberId,
        Long schoolId,
        Instant evaluatedAt,
        Set<SystemRoleType> systemRoles,
        List<AuthorizationRoleTuple> roles,
        List<AuthorizationChallengerTuple> challengers,
        Map<AuthorizationSchoolChapterKey, Long> chapters
    ) {
        return new AuthorizationSubjectSnapshot(
            new AuthorizationPrincipal.Member(memberId),
            schoolId == null ? OptionalLong.empty() : OptionalLong.of(schoolId),
            evaluatedAt,
            systemRoles,
            roles,
            challengers,
            chapters);
    }

    public static AuthorizationSubjectSnapshot anonymous(Instant evaluatedAt) {
        return withoutMember(new AuthorizationPrincipal.Anonymous(), evaluatedAt);
    }

    public static AuthorizationSubjectSnapshot system(String systemId, Instant evaluatedAt) {
        return withoutMember(new AuthorizationPrincipal.SystemPrincipal(systemId), evaluatedAt);
    }

    public static AuthorizationSubjectSnapshot capability(
        String capabilityType,
        String boundResourceId,
        Instant evaluatedAt
    ) {
        return withoutMember(
            new AuthorizationPrincipal.Capability(capabilityType, boundResourceId),
            evaluatedAt);
    }

    public boolean isSuperAdmin() {
        return systemRoles.contains(SystemRoleType.SUPER_ADMIN);
    }

    public long requireMemberId() {
        if (principal instanceof AuthorizationPrincipal.Member member) {
            return member.memberId();
        }
        throw new IllegalStateException("MEMBER principal이 아닙니다.");
    }

    private static AuthorizationSubjectSnapshot withoutMember(
        AuthorizationPrincipal principal,
        Instant evaluatedAt
    ) {
        return new AuthorizationSubjectSnapshot(
            principal,
            OptionalLong.empty(),
            evaluatedAt,
            Set.of(),
            List.of(),
            List.of(),
            Map.of());
    }

    private static void validatePrincipalScope(
        AuthorizationPrincipal principal,
        OptionalLong schoolId,
        Set<SystemRoleType> systemRoles,
        List<AuthorizationRoleTuple> roles,
        List<AuthorizationChallengerTuple> challengers,
        Map<AuthorizationSchoolChapterKey, Long> chapterIdByGisuAndSchool
    ) {
        if (principal.kind() == AuthorizationPrincipal.Kind.MEMBER) {
            return;
        }
        if (schoolId.isPresent()
            || !systemRoles.isEmpty()
            || !roles.isEmpty()
            || !challengers.isEmpty()
            || !chapterIdByGisuAndSchool.isEmpty()) {
            throw new IllegalArgumentException("MEMBER가 아닌 principal은 member authority fact를 가질 수 없습니다.");
        }
    }
}
