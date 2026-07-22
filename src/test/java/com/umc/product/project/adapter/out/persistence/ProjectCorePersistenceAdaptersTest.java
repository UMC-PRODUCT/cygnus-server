package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApprovedApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMatchingRoundRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMemberRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsProjectRow;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("Project core persistence adapters")
class ProjectCorePersistenceAdaptersTest {

    @Nested
    class ProjectAdapter {
        @Mock ProjectJpaRepository jpaRepository;
        @Mock ProjectQueryRepository queryRepository;

        @Test
        @DisplayName("단건·목록·검색·저장 계약을 위임하고 not-found를 구분한다")
        void delegates_all_operations() {
            Project project = project(1L);
            SearchProjectQuery query = org.mockito.Mockito.mock(SearchProjectQuery.class);
            @SuppressWarnings("unchecked")
            Page<Project> page = org.mockito.Mockito.mock(Page.class);
            given(jpaRepository.findById(1L)).willReturn(Optional.of(project));
            given(jpaRepository.findById(404L)).willReturn(Optional.empty());
            given(jpaRepository.findAllById(List.of(1L))).willReturn(List.of(project));
            given(jpaRepository.findByChapterIdAndStatus(2L, ProjectStatus.IN_PROGRESS))
                .willReturn(List.of(project));
            given(jpaRepository.existsByProductOwnerMemberIdAndGisuId(3L, 4L)).willReturn(true);
            given(jpaRepository.findByCreatorMemberIdAndGisuIdAndStatus(3L, 4L, ProjectStatus.DRAFT))
                .willReturn(Optional.of(project));
            given(jpaRepository.existsByCreatorMemberIdAndGisuIdAndStatus(3L, 4L, ProjectStatus.DRAFT))
                .willReturn(true);
            given(jpaRepository.existsByProductOwnerMemberIdAndGisuIdAndStatus(3L, 4L, ProjectStatus.DRAFT))
                .willReturn(true);
            given(queryRepository.search(query)).willReturn(page);
            given(jpaRepository.save(project)).willReturn(project);
            given(jpaRepository.saveAll(List.of(project))).willReturn(List.of(project));
            ProjectPersistenceAdapter sut = new ProjectPersistenceAdapter(jpaRepository, queryRepository);

            assertThat(sut.findById(1L)).containsSame(project);
            assertThat(sut.getById(1L)).isSameAs(project);
            assertThatThrownBy(() -> sut.getById(404L))
                .isInstanceOfSatisfying(ProjectDomainException.class, exception ->
                    assertThat(exception.getBaseCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
            assertThat(sut.listByIds(List.of())).isEmpty();
            assertThat(sut.listByIds(List.of(1L))).containsExactly(project);
            assertThat(sut.listByChapterIdAndStatus(2L, ProjectStatus.IN_PROGRESS)).containsExactly(project);
            assertThat(sut.existsByOwnerAndGisu(3L, 4L)).isTrue();
            assertThat(sut.findDraftByCreatorAndGisu(3L, 4L)).containsSame(project);
            assertThat(sut.existsDraftByCreatorAndGisu(3L, 4L)).isTrue();
            assertThat(sut.existsDraftByOwnerAndGisu(3L, 4L)).isTrue();
            assertThat(sut.search(query)).isSameAs(page);
            assertThat(sut.save(project)).isSameAs(project);
            assertThat(sut.saveAll(List.of(project))).containsExactly(project);
            sut.delete(project);
            then(jpaRepository).should().delete(project);
        }
    }

    @Nested
    class MatchingRoundAdapter {
        @Mock ProjectMatchingRoundJpaRepository repository;

        @Test
        @DisplayName("batch는 중복 제거·입력 순서를 보존하고 누락을 거부하며 나머지 port를 위임한다")
        void batch_and_delegation_contracts() {
            ProjectMatchingRound first = round(1L);
            ProjectMatchingRound second = round(2L);
            given(repository.findById(1L)).willReturn(Optional.of(first));
            given(repository.findById(404L)).willReturn(Optional.empty());
            given(repository.findAllById(Set.of(1L, 2L))).willReturn(List.of(second, first));
            given(repository.findAllById(List.of(2L, 1L))).willReturn(List.of(first, second));
            ProjectMatchingRoundPersistenceAdapter sut = new ProjectMatchingRoundPersistenceAdapter(repository);

            assertThat(sut.getById(1L)).isSameAs(first);
            assertThat(sut.findById(1L)).containsSame(first);
            assertThatThrownBy(() -> sut.getById(404L)).isInstanceOf(ProjectDomainException.class);
            assertThat(sut.listByIds(null)).isEmpty();
            assertThat(sut.listByIds(List.of())).isEmpty();
            assertThat(sut.listByIds(List.of(1L, 2L, 1L))).containsExactly(second, first);
            assertThat(sut.batchGetByIds(null)).isEmpty();
            assertThat(sut.batchGetByIds(List.of())).isEmpty();
            assertThat(sut.batchGetByIds(List.of(2L, 1L, 2L))).containsExactly(second, first);

            given(repository.findAllById(List.of(1L, 404L))).willReturn(List.of(first));
            assertThatThrownBy(() -> sut.batchGetByIds(List.of(1L, 404L)))
                .isInstanceOf(ProjectDomainException.class);

            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            given(repository.findAllByChapterIdOrderByStartsAtAsc(3L)).willReturn(List.of(first));
            given(repository.findAllByOrderByStartsAtAsc()).willReturn(List.of(first));
            given(repository.findOpenAt(3L, now)).willReturn(List.of(first));
            given(repository.findOverlapping(3L, now, now)).willReturn(List.of(first));
            given(repository.findOverlappingExceptId(1L, 3L, now, now)).willReturn(List.of(first));
            given(repository.findAllByAutoDecisionExecutedAtIsNullOrderByDecisionDeadlineAsc())
                .willReturn(List.of(first));
            given(repository.save(first)).willReturn(first);
            given(repository.saveAll(List.of(first))).willReturn(List.of(first));

            assertThat(sut.listByChapterId(3L)).containsExactly(first);
            assertThat(sut.listAll()).containsExactly(first);
            assertThat(sut.listOpenAt(3L, now)).containsExactly(first);
            assertThat(sut.listOverlapping(3L, now, now)).containsExactly(first);
            assertThat(sut.listOverlappingExceptId(1L, 3L, now, now)).containsExactly(first);
            assertThat(sut.listAllNotAutoDecided()).containsExactly(first);
            assertThat(sut.save(first)).isSameAs(first);
            assertThat(sut.saveAll(List.of(first))).containsExactly(first);
            sut.delete(first);
            sut.deleteAll(List.of(first));
            then(repository).should().delete(first);
            then(repository).should().deleteAll(List.of(first));
        }
    }

    @Nested
    class MemberAdapter {
        @Mock ProjectMemberJpaRepository repository;
        @Mock ProjectMemberQueryRepository queryRepository;

        @Test
        @DisplayName("활성 멤버 filter·grouping·count와 not-found 계약을 보존한다")
        void delegates_and_groups_members() {
            Project project = project(1L);
            ProjectMember member = ProjectMember.create(project, 10L, ChallengerPart.PLAN, 20L);
            given(repository.save(member)).willReturn(member);
            given(repository.saveAll(List.of(member))).willReturn(List.of(member));
            given(repository.findByProjectIdAndStatusOrderByCreatedAtAscIdAsc(1L, ProjectMemberStatus.ACTIVE))
                .willReturn(List.of(member));
            given(repository.findByProjectIdInAndStatusOrderByCreatedAtAscIdAsc(
                Set.of(1L), ProjectMemberStatus.ACTIVE)).willReturn(List.of(member));
            given(repository.findByProjectIdAndPartAndStatusOrderByCreatedAtAscIdAsc(
                1L, ChallengerPart.PLAN, ProjectMemberStatus.ACTIVE)).willReturn(List.of(member));
            given(queryRepository.listByProjectIdsAndPartGroupedByProjectId(Set.of(1L), ChallengerPart.PLAN))
                .willReturn(Map.of(1L, List.of(member)));
            given(repository.countByProjectIdGroupByPartRaw(1L, ProjectMemberStatus.ACTIVE))
                .willReturn(List.<Object[]>of(new Object[]{ChallengerPart.PLAN, 2L}));
            given(queryRepository.countByProjectIdsGroupByProjectIdAndPart(Set.of(1L)))
                .willReturn(Map.of(1L, Map.of(ChallengerPart.PLAN, 2L)));
            given(repository.findByProjectIdAndMemberId(1L, 10L)).willReturn(Optional.of(member));
            given(repository.findByProjectIdAndMemberId(1L, 404L)).willReturn(Optional.empty());
            given(repository.existsByProject_GisuIdAndMemberIdAndStatus(2L, 10L, ProjectMemberStatus.ACTIVE))
                .willReturn(true);
            given(repository.existsByProjectIdAndMemberIdAndPartAndStatus(
                1L, 10L, ChallengerPart.PLAN, ProjectMemberStatus.ACTIVE)).willReturn(true);
            given(queryRepository.listProjectIdsByActivePlanMember(Set.of(1L), 10L)).willReturn(List.of(1L));
            given(queryRepository.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
                10L, 2L, MatchingType.PLAN_DESIGN)).willReturn(Optional.of(member));
            ProjectMemberPersistenceAdapter sut = new ProjectMemberPersistenceAdapter(repository, queryRepository);

            assertThat(sut.save(member)).isSameAs(member);
            assertThat(sut.saveAll(List.of(member))).containsExactly(member);
            assertThat(sut.listByProjectId(1L)).containsExactly(member);
            assertThat(sut.listByProjectIds(Set.of(1L))).containsEntry(1L, List.of(member));
            assertThat(sut.listByProjectIdAndPart(1L, ChallengerPart.PLAN)).containsExactly(member);
            assertThat(sut.listByProjectIdsAndPartGroupedByProjectId(Set.of(1L), ChallengerPart.PLAN))
                .containsEntry(1L, List.of(member));
            assertThat(sut.countByProjectIdGroupByPart(1L)).containsEntry(ChallengerPart.PLAN, 2L);
            assertThat(sut.countByProjectIdsGroupByProjectIdAndPart(Set.of(1L))).containsKey(1L);
            assertThat(sut.findByProjectIdAndMemberId(1L, 10L)).containsSame(member);
            assertThat(sut.getByProjectIdAndMemberId(1L, 10L)).isSameAs(member);
            assertThatThrownBy(() -> sut.getByProjectIdAndMemberId(1L, 404L))
                .isInstanceOf(ProjectDomainException.class);
            assertThat(sut.existsByGisuAndMember(2L, 10L)).isTrue();
            assertThat(sut.isActivePlanMember(1L, 10L)).isTrue();
            assertThat(sut.listProjectIdsByActivePlanMember(Set.of(1L), 10L)).containsExactly(1L);
            assertThat(sut.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
                10L, 2L, MatchingType.PLAN_DESIGN)).containsSame(member);
            sut.hardDelete(30L);
            sut.deleteAllByProjectId(1L);
            then(repository).should().deleteById(30L);
            then(repository).should().deleteAllByProjectId(1L);
        }
    }

    @Nested
    class QuotaAdapter {
        @Mock ProjectPartQuotaJpaRepository repository;
        @Mock ProjectPartQuotaQueryRepository queryRepository;

        @Test
        @DisplayName("quota port 위임과 빈 삭제 short-circuit를 보존한다")
        void delegates_and_short_circuits_empty_delete() {
            ProjectPartQuota quota = ProjectPartQuota.create(project(1L), ChallengerPart.WEB, 2L, 10L);
            given(repository.findByProjectId(1L)).willReturn(List.of(quota));
            given(repository.existsByProjectIdAndPart(1L, ChallengerPart.WEB)).willReturn(true);
            given(queryRepository.listByProjectIdsGroupedByProjectId(Set.of(1L)))
                .willReturn(Map.of(1L, List.of(quota)));
            given(repository.save(quota)).willReturn(quota);
            given(repository.saveAll(List.of(quota))).willReturn(List.of(quota));
            ProjectPartQuotaPersistenceAdapter sut = new ProjectPartQuotaPersistenceAdapter(repository, queryRepository);

            assertThat(sut.listByProjectId(1L)).containsExactly(quota);
            assertThat(sut.existsByProjectIdAndPart(1L, ChallengerPart.WEB)).isTrue();
            assertThat(sut.listByProjectIdsGroupedByProjectId(Set.of(1L))).containsKey(1L);
            assertThat(sut.save(quota)).isSameAs(quota);
            assertThat(sut.saveAll(List.of(quota))).containsExactly(quota);
            sut.deleteByProjectIdAndPartIn(1L, null);
            sut.deleteByProjectIdAndPartIn(1L, List.of());
            sut.deleteByProjectIdAndPartIn(1L, Set.of(ChallengerPart.WEB));
            sut.deleteAllByProjectId(1L);

            then(repository).should().deleteByProjectIdAndPartIn(1L, Set.of(ChallengerPart.WEB));
            then(repository).should().deleteAllByProjectId(1L);
        }
    }

    @Nested
    class StatisticsAdapter {
        @Mock ProjectStatisticsQueryRepository queryRepository;

        @Test
        @DisplayName("통계 row 조회를 위임하고 없는 프로젝트만 not-found로 변환한다")
        void delegates_statistics_queries() {
            ProjectStatisticsProjectRow project = new ProjectStatisticsProjectRow(1L, 2L, 3L);
            ProjectStatisticsMatchingRoundRow round = new ProjectStatisticsMatchingRoundRow(
                4L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
            ProjectStatisticsMemberRow member = new ProjectStatisticsMemberRow(
                1L, 5L, 6L, ChallengerPart.DESIGN, ProjectMemberStatus.ACTIVE);
            ProjectStatisticsApplicationRow application = new ProjectStatisticsApplicationRow(
                1L, 6L, 7L, ProjectApplicationStatus.SUBMITTED,
                4L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
            ProjectStatisticsApprovedApplicationRow approved = new ProjectStatisticsApprovedApplicationRow(
                1L, 6L, 7L, 4L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, Instant.EPOCH);
            given(queryRepository.findProjectById(1L)).willReturn(Optional.of(project));
            given(queryRepository.findProjectById(404L)).willReturn(Optional.empty());
            given(queryRepository.listProjectsByChapterId(3L)).willReturn(List.of(project));
            given(queryRepository.listPublicProjectsByChapterId(3L)).willReturn(List.of(project));
            given(queryRepository.listMatchingRoundsByChapterId(3L)).willReturn(List.of(round));
            given(queryRepository.listActiveMembersByProjectId(1L)).willReturn(List.of(member));
            given(queryRepository.listActiveMembersByChapterId(3L)).willReturn(List.of(member));
            given(queryRepository.listActiveMembersByProjectIds(Set.of(1L))).willReturn(List.of(member));
            given(queryRepository.listPublicActiveMembersByChapterId(3L)).willReturn(List.of(member));
            given(queryRepository.listCountedApplicationsByProjectIds(Set.of(1L))).willReturn(List.of(application));
            given(queryRepository.listApprovedApplicationsByProjectIds(Set.of(1L))).willReturn(List.of(approved));
            ProjectStatisticsPersistenceAdapter sut = new ProjectStatisticsPersistenceAdapter(queryRepository);

            assertThat(sut.getProjectById(1L)).isEqualTo(project);
            assertThatThrownBy(() -> sut.getProjectById(404L)).isInstanceOf(ProjectDomainException.class);
            assertThat(sut.listProjectsByChapterId(3L)).containsExactly(project);
            assertThat(sut.listPublicProjectsByChapterId(3L)).containsExactly(project);
            assertThat(sut.listMatchingRoundsByChapterId(3L)).containsExactly(round);
            assertThat(sut.listActiveMembersByProjectId(1L)).containsExactly(member);
            assertThat(sut.listActiveMembersByChapterId(3L)).containsExactly(member);
            assertThat(sut.listActiveMembersByProjectIds(Set.of(1L))).containsExactly(member);
            assertThat(sut.listPublicActiveMembersByChapterId(3L)).containsExactly(member);
            assertThat(sut.listCountedApplicationsByProjectIds(Set.of(1L))).containsExactly(application);
            assertThat(sut.listApprovedApplicationsByProjectIds(Set.of(1L))).containsExactly(approved);
        }
    }

    private static Project project(Long id) {
        Project project = Project.createDraft(2L, 3L, 4L, 5L, 4L);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private static ProjectMatchingRound round(Long id) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "매칭", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 3L,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"),
            Instant.parse("2026-01-03T00:00:00Z")
        );
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
