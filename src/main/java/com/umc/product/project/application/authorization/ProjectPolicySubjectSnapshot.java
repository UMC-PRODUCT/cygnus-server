package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
}
