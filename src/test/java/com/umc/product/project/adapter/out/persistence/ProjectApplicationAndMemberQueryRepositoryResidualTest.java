package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ProjectApplicationQueryRepository.class, ProjectMemberQueryRepository.class})
@DisplayName("Project application·member QueryDSL 잔여 통합")
class ProjectApplicationAndMemberQueryRepositoryResidualTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-01-03T00:00:00Z");

    @Autowired TestEntityManager entityManager;
    @Autowired ProjectApplicationQueryRepository applicationRepository;
    @Autowired ProjectMemberQueryRepository memberRepository;

    private Project project;
    private ProjectApplicationForm form;
    private ProjectMatchingRound round;

    @BeforeEach
    void setUp() {
        project = persistProject(1L, 10L);
        form = entityManager.persist(ProjectApplicationForm.create(project, 100L));
        round = entityManager.persist(ProjectMatchingRound.create(
            "1차", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 10L,
            START, END, DEADLINE
        ));
    }

    @Test
    @DisplayName("지원서 단건·존재·detail·batch query는 project/member/round/status를 모두 제한한다")
    void application_lookup_and_detail_contracts() {
        ProjectApplication submitted = persistApplication(200L, ProjectApplicationStatus.SUBMITTED);
        entityManager.flush();
        entityManager.clear();

        assertThat(applicationRepository.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
            project.getId(), 200L, round.getId(), ProjectApplicationStatus.SUBMITTED))
            .isPresent();
        assertThat(applicationRepository.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
            project.getId(), 200L, round.getId(), ProjectApplicationStatus.DRAFT))
            .isEmpty();
        assertThat(applicationRepository.existsByRoundAndApplicantAndStatus(
            round.getId(), 200L, ProjectApplicationStatus.SUBMITTED)).isTrue();
        assertThat(applicationRepository.existsByRoundAndApplicantAndStatus(
            round.getId(), 999L, ProjectApplicationStatus.SUBMITTED)).isFalse();
        assertThat(applicationRepository.findByProjectIdAndApplicantMemberIdAndStatus(
            project.getId(), 200L, ProjectApplicationStatus.SUBMITTED)).isPresent();
        assertThat(applicationRepository.findByProjectIdAndApplicantMemberIdAndStatus(
            project.getId(), 200L, ProjectApplicationStatus.DRAFT)).isEmpty();
        assertThat(applicationRepository.findByIdWithDetails(submitted.getId()))
            .get().extracting(value -> value.getApplicationForm().getProject().getId())
            .isEqualTo(project.getId());
        assertThat(applicationRepository.findByIdWithDetails(999_999L)).isEmpty();
        assertThat(applicationRepository.findAllByIdInWithDetails(null)).isEmpty();
        assertThat(applicationRepository.findAllByIdInWithDetails(List.of())).isEmpty();
        assertThat(applicationRepository.findAllByIdInWithDetails(Set.of(submitted.getId())))
            .extracting(ProjectApplication::getId).containsExactly(submitted.getId());
    }

    @Test
    @DisplayName("본인 검색과 결정 대상 query는 DRAFT를 제외하고 명시 status를 적용한다")
    void application_search_and_decidable_filters() {
        ProjectApplication submitted = persistApplication(200L, ProjectApplicationStatus.SUBMITTED);
        ProjectApplication approved = persistApplication(201L, ProjectApplicationStatus.APPROVED);
        ProjectApplication rejected = persistApplication(202L, ProjectApplicationStatus.REJECTED);
        persistApplication(203L, ProjectApplicationStatus.DRAFT);
        persistApplication(204L, ProjectApplicationStatus.CANCELLED);
        entityManager.flush();
        entityManager.clear();

        assertThat(applicationRepository.searchMyApplications(
            200L, 1L, MatchingType.PLAN_DESIGN, ProjectApplicationStatus.SUBMITTED))
            .extracting(ProjectApplication::getId).containsExactly(submitted.getId());
        assertThat(applicationRepository.searchMyApplications(
            200L, 1L, MatchingType.PLAN_DEVELOPER, ProjectApplicationStatus.SUBMITTED)).isEmpty();
        assertThat(applicationRepository.searchMyApplications(
            200L, 1L, MatchingType.PLAN_DESIGN, null))
            .extracting(ProjectApplication::getId).containsExactly(submitted.getId());
        assertThat(applicationRepository.listDecidableByMatchingRoundIdAndProjectId(
            round.getId(), project.getId()))
            .extracting(ProjectApplication::getId)
            .containsExactlyInAnyOrder(submitted.getId(), approved.getId(), rejected.getId());

        assertThat(applicationRepository.searchProjectApplicationsByProjectIds(
            null, Set.of(), null, null, START)).isEmpty();
        assertThat(applicationRepository.searchProjectApplicationsByProjectIds(
            Set.of(), Set.of(), null, null, START)).isEmpty();
        assertThat(applicationRepository.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
            null, Set.of(200L))).isEmpty();
        assertThat(applicationRepository.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
            Set.of(project.getId()), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("member query는 matching part·application null·ACTIVE와 grouping/count를 보존한다")
    void member_matching_grouping_and_count_contracts() {
        ProjectMember design = entityManager.persist(
            ProjectMember.create(project, 300L, ChallengerPart.DESIGN, 1L));
        ProjectMember web = entityManager.persist(
            ProjectMember.create(project, 301L, ChallengerPart.WEB, 1L));
        ProjectMember dismissed = ProjectMember.create(project, 302L, ChallengerPart.DESIGN, 1L);
        dismissed.dismiss("종료", 1L);
        entityManager.persist(dismissed);
        entityManager.flush();
        entityManager.clear();

        assertThat(memberRepository.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
            300L, 1L, MatchingType.PLAN_DESIGN)).get()
            .extracting(ProjectMember::getId).isEqualTo(design.getId());
        assertThat(memberRepository.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
            301L, 1L, MatchingType.PLAN_DEVELOPER)).get()
            .extracting(ProjectMember::getId).isEqualTo(web.getId());
        assertThat(memberRepository.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
            300L, 99L, MatchingType.PLAN_DESIGN)).isEmpty();

        assertThat(memberRepository.listByProjectIdsAndPartGroupedByProjectId(null, ChallengerPart.DESIGN))
            .isEmpty();
        assertThat(memberRepository.listByProjectIdsAndPartGroupedByProjectId(List.of(), ChallengerPart.DESIGN))
            .isEmpty();
        assertThat(memberRepository.listByProjectIdsAndPartGroupedByProjectId(
            Set.of(project.getId()), ChallengerPart.DESIGN).get(project.getId()))
            .extracting(ProjectMember::getId).containsExactly(design.getId());
        assertThat(memberRepository.listProjectIdsByActivePlanMember(null, 300L)).isEmpty();
        assertThat(memberRepository.listProjectIdsByActivePlanMember(List.of(), 300L)).isEmpty();
        assertThat(memberRepository.countByProjectIdsGroupByProjectIdAndPart(null)).isEmpty();
        assertThat(memberRepository.countByProjectIdsGroupByProjectIdAndPart(List.of())).isEmpty();
        Map<ChallengerPart, Long> counts = memberRepository.countByProjectIdsGroupByProjectIdAndPart(
            Set.of(project.getId())).get(project.getId());
        assertThat(counts).containsEntry(ChallengerPart.DESIGN, 1L)
            .containsEntry(ChallengerPart.WEB, 1L);
    }

    private Project persistProject(Long gisuId, Long chapterId) {
        Project value = Project.createDraft(gisuId, chapterId, 1L, 2L, 1L);
        ReflectionTestUtils.setField(value, "status", ProjectStatus.IN_PROGRESS);
        return entityManager.persist(value);
    }

    private ProjectApplication persistApplication(Long applicantId, ProjectApplicationStatus status) {
        ProjectApplication value = ProjectApplication.create(form, applicantId + 1_000L, applicantId, round);
        if (status != ProjectApplicationStatus.DRAFT) {
            value.submit();
        }
        if (status == ProjectApplicationStatus.APPROVED) {
            value.forceApprove(1L, "승인");
        } else if (status == ProjectApplicationStatus.REJECTED) {
            value.forceReject(1L, "거절");
        } else if (status == ProjectApplicationStatus.CANCELLED) {
            value.cancel(1L, "취소");
        }
        return entityManager.persist(value);
    }
}
