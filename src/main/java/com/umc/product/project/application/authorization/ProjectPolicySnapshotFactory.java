package com.umc.product.project.application.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProjectPolicySnapshotFactory {

    private final Clock clock;

    public ProjectPolicySnapshotFactory(Clock clock) {
        this.clock = clock;
    }

    public ProjectPolicySubjectSnapshot member(
        long memberId,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> chapterIdByGisuAndSchool
    ) {
        return member(memberId, false, roles, challengers, chapterIdByGisuAndSchool);
    }

    public ProjectPolicySubjectSnapshot member(
        long memberId,
        boolean superAdmin,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> chapterIdByGisuAndSchool
    ) {
        Instant evaluatedAt = clock.instant();
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(memberId),
            evaluatedAt,
            superAdmin,
            roles,
            challengers,
            chapterIdByGisuAndSchool
        );
    }

    public ProjectPolicySubjectSnapshot system(String systemId) {
        Instant evaluatedAt = clock.instant();
        return ProjectPolicySubjectSnapshot.system(systemId, evaluatedAt);
    }
}
