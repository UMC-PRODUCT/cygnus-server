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

import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ProjectApplicationQueryRepository.class})
class ProjectApplicationQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProjectApplicationQueryRepository sut;

    private Project project;
    private ProjectApplicationForm form;

    @BeforeEach
    void setUp() {
        project = persistProject("프로젝트 알파", 10L);
        form = ProjectApplicationForm.create(project, 500L);
        em.persist(form);
    }

    @Test
    @DisplayName("프로젝트_멤버별_APPROVED_지원서_중_최신_매칭차수만_반환한다")
    void listLatestApprovedMatchedRoundsByProjectIdsAndMemberIdsReturnsLatestApprovedOnly() {
        Long memberId = 200L;
        ProjectMatchingRound oldApprovedRound = persistRound(
            "1차",
            MatchingType.PLAN_DEVELOPER,
            MatchingPhase.FIRST,
            Instant.parse("2026-05-01T00:00:00Z")
        );
        ProjectMatchingRound latestApprovedRound = persistRound(
            "2차",
            MatchingType.PLAN_DEVELOPER,
            MatchingPhase.SECOND,
            Instant.parse("2026-05-03T00:00:00Z")
        );
        ProjectMatchingRound tieApprovedRound = persistRound(
            "디자인 1차",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            Instant.parse("2026-05-03T00:00:00Z")
        );
        ProjectMatchingRound rejectedRound = persistRound(
            "3차",
            MatchingType.PLAN_DEVELOPER,
            MatchingPhase.THIRD,
            Instant.parse("2026-05-05T00:00:00Z")
        );
        ProjectMatchingRound submittedRound = persistRound(
            "디자인 2차",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.SECOND,
            Instant.parse("2026-05-06T00:00:00Z")
        );

        persistApplication(memberId, oldApprovedRound, ProjectApplicationStatus.APPROVED);
        ProjectApplication latestApproved = persistApplication(memberId, latestApprovedRound,
            ProjectApplicationStatus.APPROVED);
        ProjectApplication tieApproved = persistApplication(memberId, tieApprovedRound,
            ProjectApplicationStatus.APPROVED);
        persistApplication(memberId, rejectedRound, ProjectApplicationStatus.REJECTED);
        persistApplication(memberId, submittedRound, ProjectApplicationStatus.SUBMITTED);
        persistApplication(201L, rejectedRound, ProjectApplicationStatus.REJECTED);
        em.flush();
        em.clear();

        List<ProjectMemberMatchedRoundInfo> result = sut.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
            Set.of(project.getId()),
            Set.of(memberId, 201L)
        );

        assertThat(result).containsExactly(new ProjectMemberMatchedRoundInfo(
            project.getId(),
            memberId,
            tieApprovedRound.getId(),
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST
        ));
        assertThat(tieApproved.getId()).isGreaterThan(latestApproved.getId());
    }

    @Test
    @DisplayName("searchProjectApplications_지원_종료(endsAt<now)된_차수의_지원서만_반환한다")
    void searchProjectApplicationsReturnsOnlyEndedRounds() {
        // given - endedRound 는 종료, ongoingRound 는 진행 중
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        ProjectMatchingRound endedRound = persistRound(
            "종료 차수", MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST,
            now.minusSeconds(7_200)); // endsAt = now - 3600 (과거)
        ProjectMatchingRound ongoingRound = persistRound(
            "진행 차수", MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            now.minusSeconds(1_800)); // endsAt = now + 1800 (미래)

        ProjectApplication endedApp = persistApplication(200L, endedRound, ProjectApplicationStatus.SUBMITTED);
        persistApplication(201L, ongoingRound, ProjectApplicationStatus.SUBMITTED);
        em.flush();
        em.clear();

        // when - 전체 조회 (matchingRoundId = null)
        List<ProjectApplication> result = sut.searchProjectApplications(
            project.getId(), null, null, now, false);

        // then - 종료된 차수의 지원서만 반환
        assertThat(result)
            .extracting(ProjectApplication::getId)
            .containsExactly(endedApp.getId());
    }

    @Test
    @DisplayName("searchProjectApplications_특정_차수가_아직_진행_중이면_빈_리스트")
    void searchProjectApplicationsEmptyWhenRoundOngoing() {
        // given
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        ProjectMatchingRound ongoingRound = persistRound(
            "진행 차수", MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            now.minusSeconds(1_800)); // endsAt = now + 1800 (미래)
        persistApplication(200L, ongoingRound, ProjectApplicationStatus.SUBMITTED);
        em.flush();
        em.clear();

        // when
        List<ProjectApplication> result = sut.searchProjectApplications(
            project.getId(), ongoingRound.getId(), null, now, false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchProjectApplications_includeOngoingMatchingRounds_true면_진행_중_차수의_지원서도_반환한다")
    void searchProjectApplicationsReturnsOngoingRoundsWhenIncluded() {
        // given
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        ProjectMatchingRound ongoingRound = persistRound(
            "진행 차수", MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            now.minusSeconds(1_800)); // endsAt = now + 1800 (미래)
        ProjectApplication ongoingApp = persistApplication(200L, ongoingRound, ProjectApplicationStatus.SUBMITTED);
        em.flush();
        em.clear();

        // when
        List<ProjectApplication> result = sut.searchProjectApplications(
            project.getId(), ongoingRound.getId(), null, now, true);

        // then
        assertThat(result)
            .extracting(ProjectApplication::getId)
            .containsExactly(ongoingApp.getId());
    }

    @Test
    @DisplayName("searchProjectApplicationsByProjectIds_includeOngoingProjectIds에_포함된_프로젝트만_진행중_차수_지원서를_반환한다")
    void searchProjectApplicationsByProjectIdsIncludesOngoingOnlyForAllowedProjects() {
        // given
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        Project projectB = persistProject("프로젝트 베타", 20L);
        ProjectApplicationForm formB = ProjectApplicationForm.create(projectB, 600L);
        em.persist(formB);

        ProjectMatchingRound endedRound = persistRound(
            "종료 차수", MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST,
            now.minusSeconds(7_200));
        ProjectMatchingRound ongoingRound = persistRound(
            "진행 차수", MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND,
            now.minusSeconds(1_800));

        ProjectApplication projectAEnded = persistApplication(200L, endedRound, ProjectApplicationStatus.SUBMITTED);
        persistApplication(201L, ongoingRound, ProjectApplicationStatus.SUBMITTED);
        persistApplication(202L, endedRound, ProjectApplicationStatus.DRAFT);
        ProjectApplication projectBEnded = persistApplication(
            formB, 300L, endedRound, ProjectApplicationStatus.APPROVED);
        ProjectApplication projectBOngoing = persistApplication(
            formB, 301L, ongoingRound, ProjectApplicationStatus.SUBMITTED);
        em.flush();
        em.clear();

        // when
        List<ProjectApplication> result = sut.searchProjectApplicationsByProjectIds(
            Set.of(project.getId(), projectB.getId()),
            Set.of(projectB.getId()),
            null,
            null,
            now
        );

        // then
        assertThat(result)
            .extracting(ProjectApplication::getId)
            .containsExactly(projectAEnded.getId(), projectBEnded.getId(), projectBOngoing.getId());
    }

    @Test
    @DisplayName("searchProjectApplicationsByProjectIds_matchingRoundId_status_필터를_적용한다")
    void searchProjectApplicationsByProjectIdsAppliesMatchingRoundAndStatusFilters() {
        // given
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        Project projectB = persistProject("프로젝트 베타", 20L);
        ProjectApplicationForm formB = ProjectApplicationForm.create(projectB, 600L);
        em.persist(formB);

        ProjectMatchingRound firstRound = persistRound(
            "1차", MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST,
            now.minusSeconds(7_200));
        ProjectMatchingRound secondRound = persistRound(
            "2차", MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND,
            now.minusSeconds(7_200));

        persistApplication(200L, firstRound, ProjectApplicationStatus.SUBMITTED);
        ProjectApplication approved = persistApplication(201L, firstRound, ProjectApplicationStatus.APPROVED);
        persistApplication(formB, 300L, firstRound, ProjectApplicationStatus.REJECTED);
        persistApplication(formB, 301L, secondRound, ProjectApplicationStatus.APPROVED);
        em.flush();
        em.clear();

        // when
        List<ProjectApplication> result = sut.searchProjectApplicationsByProjectIds(
            Set.of(project.getId(), projectB.getId()),
            Set.of(),
            firstRound.getId(),
            ProjectApplicationStatus.APPROVED,
            now
        );

        // then
        assertThat(result)
            .extracting(ProjectApplication::getId)
            .containsExactly(approved.getId());
    }

    private Project persistProject(String name, Long ownerId) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "gisuId", 1L);
        ReflectionTestUtils.setField(project, "chapterId", 1L);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "name", name);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerId);
        ReflectionTestUtils.setField(project, "productOwnerSchoolId", 1L);
        ReflectionTestUtils.setField(project, "creatorMemberId", ownerId);
        em.persist(project);
        return project;
    }

    private ProjectMatchingRound persistRound(
        String name,
        MatchingType type,
        MatchingPhase phase,
        Instant startsAt
    ) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            name,
            null,
            type,
            phase,
            1L,
            startsAt,
            startsAt.plusSeconds(3_600),
            startsAt.plusSeconds(7_200)
        );
        em.persist(round);
        return round;
    }

    private ProjectApplication persistApplication(
        Long applicantMemberId,
        ProjectMatchingRound round,
        ProjectApplicationStatus status
    ) {
        return persistApplication(form, applicantMemberId, round, status);
    }

    private ProjectApplication persistApplication(
        ProjectApplicationForm applicationForm,
        Long applicantMemberId,
        ProjectMatchingRound round,
        ProjectApplicationStatus status
    ) {
        ProjectApplication application = ProjectApplication.create(
            applicationForm, applicantMemberId * 10, applicantMemberId, round);
        ReflectionTestUtils.setField(application, "status", status);
        em.persist(application);
        return application;
    }
}
