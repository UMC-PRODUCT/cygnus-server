package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class ProjectPolicyRelationResolverTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T00:00:00Z");

    private final ProjectPolicyRelationResolver resolver = new ProjectPolicyRelationResolver();

    @Test
    @DisplayName("기수 역할은 Gisu start 포함 end 제외로 판정한다")
    void roleValidityUsesHalfOpenGisuPeriod() {
        ProjectPolicyRoleTuple central = role(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 99L, START, END);
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .projectTarget(99L, 7L)
            .build();

        assertThat(resolveAt(START.minusNanos(1), List.of(central), resource)
            .activeCentralCoreInResourceGisu()).isFalse();
        assertThat(resolveAt(START, List.of(central), resource).activeCentralCoreInResourceGisu()).isTrue();
        assertThat(resolveAt(END.minusNanos(1), List.of(central), resource)
            .activeCentralCoreInResourceGisu()).isTrue();
        assertThat(resolveAt(END, List.of(central), resource).activeCentralCoreInResourceGisu()).isFalse();
    }

    @Test
    @DisplayName("active SUPER_ADMIN은 자신의 Gisu와 다른 resource에도 전역 권한을 가진다")
    void activeSuperAdminIsGlobal() {
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), START, true, List.of(), List.of(), Map.of());
        ProjectPolicyRelationFacts facts = resolver.resolve(
            subject, ProjectPolicyResourceContext.builder().projectTarget(999L, 77L).build());

        assertThat(facts.activeSuperAdmin()).isTrue();
        assertThat(facts.activeCentralCoreInResourceGisu()).isFalse();
    }

    @Test
    @DisplayName("서로 다른 role tuple의 gisu와 organization을 섞어 지부장 권한을 만들 수 없다")
    void doesNotCrossMixRoleTuples() {
        List<ProjectPolicyRoleTuple> roles = List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 20L, 10L, START, END),
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 20L, START, END)
        );

        ProjectPolicyRelationFacts facts = resolveAt(START, roles,
            ProjectPolicyResourceContext.builder().projectTarget(10L, 10L).build());

        assertThat(facts.activeChapterPresidentForResource()).isFalse();
    }

    @Test
    @DisplayName("지부장과 학교 회장단의 managed chapter set을 분리해 public scope 상승을 막는다")
    void separatesChapterPresidentAndSchoolCoreManagedSets() {
        List<ProjectPolicyRoleTuple> roles = List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L, START, END),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 200L, 1L, START, END)
        );
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), START, roles, List.of(),
            Map.of(new ProjectPolicySchoolChapterKey(1L, 200L), 20L));

        ProjectPolicyRelationFacts facts = resolver.resolve(subject,
            ProjectPolicyResourceContext.builder().gisuScope(1L).build());

        assertThat(facts.managedChapterIdsInResourceGisu()).containsExactly(10L);
        assertThat(facts.managedSchoolCoreChapterIdsInResourceGisu()).containsExactly(20L);
    }

    @Test
    @DisplayName("프로젝트 위임 생성의 학교 운영진은 동일 role tuple의 Gisu·기간·대상 학교를 모두 만족해야 한다")
    void projectCreateSchoolCoreRequiresExactTargetSchool() {
        ProjectPolicyRoleTuple schoolPresident = role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 200L, 1L, START, END);
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), START, List.of(schoolPresident), List.of(),
            Map.of(new ProjectPolicySchoolChapterKey(1L, 200L), 10L));
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .projectTarget(1L, 10L)
            .creatorMemberId(99L)
            .build();

        ProjectPolicyRelationFacts exactSchool = resolver.resolve(subject, resource, Optional.of(200L));
        ProjectPolicyRelationFacts sameChapterOtherSchool =
            resolver.resolve(subject, resource, Optional.of(201L));

        assertThat(exactSchool.activeSchoolCoreForResource()).isTrue();
        assertThat(sameChapterOtherSchool.activeSchoolCoreForResource()).isFalse();
    }

    @Test
    @DisplayName("PLAN challenger의 active 사실도 Gisu 기간만 사용한다")
    void activePlanChallengerUsesGisuPeriod() {
        ProjectPolicyChallengerTuple challenger = new ProjectPolicyChallengerTuple(
            1L, 3L, 30L, ChallengerPart.PLAN, START, END);
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder().projectTarget(3L, 30L).build();

        ProjectPolicyRelationFacts atStart = resolver.resolve(subject(START, List.of(), List.of(challenger)), resource);
        ProjectPolicyRelationFacts atEnd = resolver.resolve(subject(END, List.of(), List.of(challenger)), resource);

        assertThat(atStart.activePlanChallengerInResourceGisu()).isTrue();
        assertThat(atEnd.activePlanChallengerInResourceGisu()).isFalse();
        assertThat(atEnd.challengerInResourceGisu()).isTrue();
    }

    private ProjectPolicyRelationFacts resolveAt(
        Instant evaluatedAt,
        List<ProjectPolicyRoleTuple> roles,
        ProjectPolicyResourceContext resource
    ) {
        return resolver.resolve(subject(evaluatedAt, roles, List.of()), resource);
    }

    private ProjectPolicySubjectSnapshot subject(
        Instant evaluatedAt,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), evaluatedAt, roles, challengers, Map.of());
    }

    private ProjectPolicyRoleTuple role(
        ChallengerRoleType type,
        OrganizationType organizationType,
        Long organizationId,
        long gisuId,
        Instant start,
        Instant end
    ) {
        return new ProjectPolicyRoleTuple(type, organizationType, organizationId, null, gisuId, start, end);
    }
}
