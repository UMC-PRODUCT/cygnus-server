package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.umc.product.authorization.domain.AuthorizationPrincipal;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public record ProjectPolicySubjectSnapshot(
    ProjectPolicyPrincipal principal,
    Instant evaluatedAt,
    boolean superAdmin,
    List<ProjectPolicyRoleTuple> roles,
    List<ProjectPolicyChallengerTuple> challengers,
    Map<ProjectPolicySchoolChapterKey, Long> chapterIdByGisuAndSchool
) {
    public ProjectPolicySubjectSnapshot {
        Objects.requireNonNull(principal);
        Objects.requireNonNull(evaluatedAt);
        roles = List.copyOf(roles);
        challengers = List.copyOf(challengers);
        chapterIdByGisuAndSchool = Map.copyOf(chapterIdByGisuAndSchool);
    }

    public ProjectPolicySubjectSnapshot(
        ProjectPolicyPrincipal principal,
        Instant evaluatedAt,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> chapterIdByGisuAndSchool
    ) {
        this(principal, evaluatedAt, false, roles, challengers, chapterIdByGisuAndSchool);
    }

    public static ProjectPolicySubjectSnapshot system(String systemId, Instant evaluatedAt) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.SystemPrincipal(systemId), evaluatedAt, false, List.of(), List.of(), Map.of());
    }

    public static ProjectPolicySubjectSnapshot from(AuthorizationSubjectSnapshot source) {
        ProjectPolicyPrincipal projectPrincipal = switch (source.principal()) {
            case AuthorizationPrincipal.Member member -> new ProjectPolicyPrincipal.Member(member.memberId());
            case AuthorizationPrincipal.SystemPrincipal system ->
                new ProjectPolicyPrincipal.SystemPrincipal(system.systemId());
            case AuthorizationPrincipal.Anonymous ignored ->
                throw new IllegalArgumentException("Project policy는 ANONYMOUS principal을 아직 지원하지 않습니다.");
            case AuthorizationPrincipal.Capability ignored ->
                throw new IllegalArgumentException("Project policy는 CAPABILITY principal을 아직 지원하지 않습니다.");
        };
        List<ProjectPolicyRoleTuple> roleTuples = source.roles().stream()
            .map(role -> new ProjectPolicyRoleTuple(
                role.roleType(),
                role.organizationType(),
                role.organizationId(),
                role.responsiblePart(),
                role.gisuId(),
                role.gisuStartAt(),
                role.gisuEndAt()))
            .toList();
        List<ProjectPolicyChallengerTuple> challengerTuples = source.challengers().stream()
            .map(challenger -> new ProjectPolicyChallengerTuple(
                challenger.challengerId(),
                challenger.gisuId(),
                challenger.chapterId(),
                challenger.part(),
                challenger.gisuStartAt(),
                challenger.gisuEndAt()))
            .toList();
        Map<ProjectPolicySchoolChapterKey, Long> chapters = source.chapterIdByGisuAndSchool().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> new ProjectPolicySchoolChapterKey(
                    entry.getKey().gisuId(),
                    entry.getKey().schoolId()),
                Map.Entry::getValue));
        return new ProjectPolicySubjectSnapshot(
            projectPrincipal,
            source.evaluatedAt(),
            source.isSuperAdmin(),
            roleTuples,
            challengerTuples,
            chapters);
    }
}
