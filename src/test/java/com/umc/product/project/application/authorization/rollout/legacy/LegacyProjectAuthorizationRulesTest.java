package com.umc.product.project.application.authorization.rollout.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class LegacyProjectAuthorizationRulesTest {

    private static final Instant EXPIRED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-02T00:00:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private final LegacyProjectAuthorizationRules rules = new LegacyProjectAuthorizationRules();

    @Test
    @DisplayName("legacy Project READ는 SystemRole SUPER_ADMIN을 기수와 무관하게 허용한다")
    void projectReadAllowsSystemSuperAdminGlobally() {
        // Given
        ProjectPolicySubjectSnapshot subject = superAdminSubject();
        ProjectPolicyResourceContext resource = project(ProjectStatus.PENDING_REVIEW, 10L, 20L).build();

        // When
        boolean allowed = rules.canReadProject(subject, resource);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("legacy Project READ는 같은 기수의 다른 지부장도 검토 상태를 조회한다")
    void projectReadAllowsChapterPresidentFromAnotherChapter() {
        // Given
        ProjectPolicySubjectSnapshot subject = subject(List.of(role(
            ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 999L, 10L)));
        ProjectPolicyResourceContext resource = project(ProjectStatus.ABORTED, 10L, 20L).build();

        // When
        boolean allowed = rules.canReadProject(subject, resource);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("legacy Project CREATE의 actor gate는 다른 기수 PLAN 이력도 허용한다")
    void projectCreateActorGateAllowsPlanHistoryFromAnotherGisu() {
        // Given
        ProjectPolicySubjectSnapshot subject = subject(
            List.of(),
            List.of(challenger(99L, ChallengerPart.PLAN)),
            Map.of()
        );

        // When
        boolean allowed = rules.canEnterProjectCreate(subject);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("legacy delegated Project CREATE는 학교 운영진의 학교가 target 학교와 정확히 같아야 한다")
    void delegatedProjectCreateRequiresExactTargetSchool() {
        // Given
        ProjectPolicySubjectSnapshot subject = subject(List.of(role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, 10L)));
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .projectTarget(10L, 20L)
            .creatorMemberId(99L)
            .build();

        // When
        boolean sameSchool = rules.canAssignProjectOwner(subject, resource, 30L);
        boolean anotherSchoolInSameChapter = rules.canAssignProjectOwner(subject, resource, 31L);

        // Then
        assertThat(sameSchool).isTrue();
        assertThat(anotherSchoolInSameChapter).isFalse();
    }

    @Test
    @DisplayName("legacy Application READ는 만료된 같은 기수 중앙 운영진 역할도 허용한다")
    void applicationReadAllowsExpiredCentralCore() {
        // Given
        ProjectPolicySubjectSnapshot subject = subject(List.of(role(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 10L)));
        ProjectPolicyResourceContext resource = project(ProjectStatus.IN_PROGRESS, 10L, 20L)
            .application(200L, ProjectApplicationStatus.SUBMITTED, 40L)
            .build();

        // When
        boolean allowed = rules.canReadApplication(subject, resource);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("legacy Form READ는 REST 단건과 GraphQL batch의 타 기수 SUPER_ADMIN 처리가 다르다")
    void formReadPreservesRestAndGraphQlSuperAdminDifference() {
        // Given
        ProjectPolicySubjectSnapshot subject = superAdminSubject();
        ProjectPolicyResourceContext resource = project(ProjectStatus.IN_PROGRESS, 10L, 20L).build();

        // When
        boolean restFullView = rules.canViewFullForm(subject, resource, false);
        boolean graphQlBatchFullView = rules.canViewFullForm(subject, resource, true);

        // Then
        assertThat(restFullView).isFalse();
        assertThat(graphQlBatchFullView).isTrue();
    }

    private ProjectPolicySubjectSnapshot subject(List<ProjectPolicyRoleTuple> roles) {
        return subject(roles, List.of(), Map.of());
    }

    private ProjectPolicySubjectSnapshot superAdminSubject() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(50L),
            EVALUATED_AT,
            true,
            List.of(),
            List.of(),
            Map.of()
        );
    }

    private ProjectPolicySubjectSnapshot subject(
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> schoolChapters
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(50L),
            EVALUATED_AT,
            roles,
            challengers,
            schoolChapters
        );
    }

    private ProjectPolicyRoleTuple role(
        ChallengerRoleType type,
        OrganizationType organizationType,
        Long organizationId,
        long gisuId
    ) {
        return new ProjectPolicyRoleTuple(
            type, organizationType, organizationId, null, gisuId, STARTED_AT, EXPIRED_AT);
    }

    private ProjectPolicyChallengerTuple challenger(long gisuId, ChallengerPart part) {
        return new ProjectPolicyChallengerTuple(70L, gisuId, 20L, part, STARTED_AT, EXPIRED_AT);
    }

    private ProjectPolicyResourceContext.Builder project(
        ProjectStatus status,
        long gisuId,
        long chapterId
    ) {
        return ProjectPolicyResourceContext.builder()
            .project(100L, gisuId, chapterId, status)
            .creatorMemberId(60L)
            .productOwnerMemberId(60L);
    }
}
