package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ProjectStatisticsQueryRepository.class, ProjectPartQuotaQueryRepository.class})
@DisplayName("Project statistics·quota QueryDSL 통합")
class ProjectStatisticsAndQuotaQueryRepositoryTest {

    private static final Long CHAPTER_ID = 10L;

    @Autowired TestEntityManager entityManager;
    @Autowired ProjectStatisticsQueryRepository statisticsRepository;
    @Autowired ProjectPartQuotaQueryRepository quotaRepository;

    private Project publicProject;
    private Project completedProject;
    private Project draftProject;
    private ProjectMatchingRound round;

    @BeforeEach
    void setUp() {
        publicProject = persistProject(ProjectStatus.IN_PROGRESS, CHAPTER_ID, 100L);
        completedProject = persistProject(ProjectStatus.COMPLETED, CHAPTER_ID, 101L);
        draftProject = persistProject(ProjectStatus.DRAFT, CHAPTER_ID, 102L);
        persistProject(ProjectStatus.IN_PROGRESS, 99L, 103L);
        round = entityManager.persist(ProjectMatchingRound.create(
            "1차", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, CHAPTER_ID,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"),
            Instant.parse("2026-01-03T00:00:00Z")
        ));
    }

    @Test
    @DisplayName("빈 projectIds는 quota·member·application query를 단축한다")
    void empty_project_ids_short_circuit() {
        assertThat(quotaRepository.listByProjectIdsGroupedByProjectId(null)).isEmpty();
        assertThat(quotaRepository.listByProjectIdsGroupedByProjectId(List.of())).isEmpty();
        assertThat(statisticsRepository.listActiveMembersByProjectIds(null)).isEmpty();
        assertThat(statisticsRepository.listActiveMembersByProjectIds(List.of())).isEmpty();
        assertThat(statisticsRepository.listCountedApplicationsByProjectIds(null)).isEmpty();
        assertThat(statisticsRepository.listCountedApplicationsByProjectIds(List.of())).isEmpty();
        assertThat(statisticsRepository.listApprovedApplicationsByProjectIds(null)).isEmpty();
        assertThat(statisticsRepository.listApprovedApplicationsByProjectIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("project·round 통계 row는 chapter·공개 상태·정렬 계약을 보존한다")
    void project_and_round_rows_preserve_filters() {
        entityManager.flush();
        entityManager.clear();

        assertThat(statisticsRepository.findProjectById(publicProject.getId()))
            .get().extracting(row -> row.chapterId()).isEqualTo(CHAPTER_ID);
        assertThat(statisticsRepository.findProjectById(999_999L)).isEmpty();
        assertThat(statisticsRepository.listProjectsByChapterId(CHAPTER_ID))
            .extracting(row -> row.projectId())
            .containsExactly(publicProject.getId(), completedProject.getId(), draftProject.getId());
        assertThat(statisticsRepository.listPublicProjectsByChapterId(CHAPTER_ID))
            .extracting(row -> row.projectId())
            .containsExactly(publicProject.getId(), completedProject.getId());
        assertThat(statisticsRepository.listMatchingRoundsByChapterId(CHAPTER_ID))
            .singleElement()
            .satisfies(row -> {
                assertThat(row.matchingRoundId()).isEqualTo(round.getId());
                assertThat(row.matchingRoundType()).isEqualTo(MatchingType.PLAN_DESIGN);
            });
    }

    @Test
    @DisplayName("member 통계는 ACTIVE·공개 project·matching 대상 part만 포함한다")
    void member_rows_preserve_active_and_public_filters() {
        ProjectMember web = entityManager.persist(
            ProjectMember.create(publicProject, 200L, ChallengerPart.WEB, 1L));
        ProjectMember plan = entityManager.persist(
            ProjectMember.create(publicProject, 201L, ChallengerPart.PLAN, 1L));
        ProjectMember dismissed = ProjectMember.create(publicProject, 202L, ChallengerPart.DESIGN, 1L);
        dismissed.dismiss("종료", 1L);
        entityManager.persist(dismissed);
        ProjectMember draftWeb = entityManager.persist(
            ProjectMember.create(draftProject, 203L, ChallengerPart.WEB, 1L));
        entityManager.flush();
        entityManager.clear();

        assertThat(statisticsRepository.listActiveMembersByProjectId(publicProject.getId()))
            .extracting(row -> row.projectMemberId())
            .containsExactly(web.getId(), plan.getId());
        assertThat(statisticsRepository.listActiveMembersByChapterId(CHAPTER_ID))
            .extracting(row -> row.projectMemberId())
            .containsExactly(web.getId(), plan.getId(), draftWeb.getId());
        assertThat(statisticsRepository.listActiveMembersByProjectIds(
            Set.of(publicProject.getId(), draftProject.getId())))
            .extracting(row -> row.projectMemberId())
            .containsExactly(web.getId(), plan.getId(), draftWeb.getId());
        assertThat(statisticsRepository.listPublicActiveMembersByChapterId(CHAPTER_ID))
            .extracting(row -> row.projectMemberId())
            .containsExactly(web.getId());
    }

    @Test
    @DisplayName("quota는 projectId로 grouping하고 application 통계는 counted·approved 상태를 구분한다")
    void quota_and_application_rows_preserve_status_filters() {
        ProjectPartQuota firstQuota = entityManager.persist(
            ProjectPartQuota.create(publicProject, ChallengerPart.WEB, 2L, 1L));
        ProjectPartQuota secondQuota = entityManager.persist(
            ProjectPartQuota.create(completedProject, ChallengerPart.DESIGN, 1L, 1L));
        ProjectApplicationForm form = entityManager.persist(
            ProjectApplicationForm.create(publicProject, 500L));
        ProjectApplication submitted = persistApplication(form, 300L, ProjectApplicationStatus.SUBMITTED);
        ProjectApplication approved = persistApplication(form, 301L, ProjectApplicationStatus.APPROVED);
        persistApplication(form, 302L, ProjectApplicationStatus.DRAFT);
        persistApplication(form, 303L, ProjectApplicationStatus.CANCELLED);
        entityManager.flush();
        entityManager.clear();

        assertThat(quotaRepository.listByProjectIdsGroupedByProjectId(
            Set.of(publicProject.getId(), completedProject.getId())))
            .satisfies(grouped -> {
                assertThat(grouped.get(publicProject.getId()))
                    .extracting(ProjectPartQuota::getId).containsExactly(firstQuota.getId());
                assertThat(grouped.get(completedProject.getId()))
                    .extracting(ProjectPartQuota::getId).containsExactly(secondQuota.getId());
            });
        assertThat(statisticsRepository.listCountedApplicationsByProjectIds(Set.of(publicProject.getId())))
            .extracting(row -> row.applicationId())
            .containsExactly(submitted.getId(), approved.getId());
        assertThat(statisticsRepository.listApprovedApplicationsByProjectIds(Set.of(publicProject.getId())))
            .singleElement()
            .satisfies(row -> {
                assertThat(row.applicationId()).isEqualTo(approved.getId());
                assertThat(row.matchingRoundStartsAt()).isEqualTo(round.getStartsAt());
            });
    }

    private Project persistProject(ProjectStatus status, Long chapterId, Long ownerId) {
        Project project = Project.createDraft(1L, chapterId, ownerId, 1L, ownerId);
        ReflectionTestUtils.setField(project, "status", status);
        return entityManager.persist(project);
    }

    private ProjectApplication persistApplication(
        ProjectApplicationForm form,
        Long applicantId,
        ProjectApplicationStatus status
    ) {
        ProjectApplication application = ProjectApplication.create(form, applicantId + 1_000L, applicantId, round);
        ReflectionTestUtils.setField(application, "status", status);
        return entityManager.persist(application);
    }
}
