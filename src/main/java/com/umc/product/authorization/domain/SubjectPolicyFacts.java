package com.umc.product.authorization.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public record SubjectPolicyFacts(
    Instant evaluatedAt,
    List<RolePolicyFact> roles,
    List<ChallengerPolicyFact> challengers,
    Map<SchoolChapterKey, Long> chapterIdByGisuAndSchool
) {

    public SubjectPolicyFacts {
        Objects.requireNonNull(evaluatedAt);
        roles = List.copyOf(roles);
        challengers = List.copyOf(challengers);
        chapterIdByGisuAndSchool = Map.copyOf(chapterIdByGisuAndSchool);
    }

    public SubjectAttributes toSubjectAttributes(long memberId, Long schoolId) {
        return toSubjectAttributes(memberId, schoolId, Set.of());
    }

    public SubjectAttributes toSubjectAttributes(
        long memberId,
        Long schoolId,
        Set<SystemRoleType> systemRoles
    ) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .gisuChallengerInfos(challengers.stream()
                .map(ChallengerPolicyFact::toSubjectAttribute)
                .toList())
            .roleAttributes(roles.stream()
                .map(RolePolicyFact::toSubjectAttribute)
                .toList())
            .systemRoles(systemRoles)
            .policyFacts(this)
            .build();
    }

    public AuthorizationSubjectSnapshot toAuthorizationSubjectSnapshot(
        long memberId,
        Long schoolId,
        Set<SystemRoleType> systemRoles
    ) {
        List<AuthorizationRoleTuple> roleTuples = roles.stream()
            .map(role -> new AuthorizationRoleTuple(
                role.roleType(),
                role.organizationType(),
                role.organizationId(),
                role.responsiblePart(),
                role.gisuId(),
                role.gisuStartAt(),
                role.gisuEndAt()))
            .toList();
        List<AuthorizationChallengerTuple> challengerTuples = challengers.stream()
            .map(challenger -> new AuthorizationChallengerTuple(
                challenger.challengerId(),
                challenger.gisuId(),
                challenger.chapterId(),
                challenger.part(),
                challenger.gisuStartAt(),
                challenger.gisuEndAt()))
            .toList();
        Map<AuthorizationSchoolChapterKey, Long> chapters = chapterIdByGisuAndSchool.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> new AuthorizationSchoolChapterKey(
                    entry.getKey().gisuId(),
                    entry.getKey().schoolId()),
                Map.Entry::getValue));
        return AuthorizationSubjectSnapshot.member(
            memberId,
            schoolId,
            evaluatedAt,
            systemRoles,
            roleTuples,
            challengerTuples,
            chapters);
    }

    public record RolePolicyFact(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        ChallengerPart responsiblePart,
        long gisuId,
        Instant gisuStartAt,
        Instant gisuEndAt
    ) {

        public RoleAttribute toSubjectAttribute() {
            return new RoleAttribute(roleType, organizationType, organizationId, responsiblePart, gisuId);
        }
    }

    public record ChallengerPolicyFact(
        long challengerId,
        long gisuId,
        long chapterId,
        ChallengerPart part,
        Instant gisuStartAt,
        Instant gisuEndAt
    ) {

        public SubjectAttributes.GisuChallengerInfo toSubjectAttribute() {
            return SubjectAttributes.GisuChallengerInfo.builder()
                .challengerId(challengerId)
                .gisuId(gisuId)
                .chapterId(chapterId)
                .part(part)
                .build();
        }
    }

    public record SchoolChapterKey(long gisuId, long schoolId) {}
}
