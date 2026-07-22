package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Project application persistence adapters 잔여 계약")
class ProjectApplicationPersistenceAdaptersResidualTest {

    @Nested
    class ApplicationFormAdapter {
        @Mock ProjectApplicationFormJpaRepository repository;

        @Test
        @DisplayName("batch는 빈 입력을 단축하고 프로젝트별 최초 form을 보존하며 CRUD를 위임한다")
        void batch_and_crud_contracts() {
            Project project = project(1L);
            ProjectApplicationForm first = applicationForm(10L, project, 100L);
            ProjectApplicationForm duplicate = applicationForm(11L, project, 101L);
            given(repository.existsByProjectId(1L)).willReturn(true);
            given(repository.findFirstByProjectIdOrderByIdAsc(1L)).willReturn(Optional.of(first));
            given(repository.findAllByProjectIds(Set.of(1L))).willReturn(List.of(first, duplicate));
            given(repository.save(first)).willReturn(first);
            given(repository.saveAll(List.of(first))).willReturn(List.of(first));
            ProjectApplicationFormPersistenceAdapter sut = new ProjectApplicationFormPersistenceAdapter(repository);

            assertThat(sut.existsByProjectId(1L)).isTrue();
            assertThat(sut.findByProjectId(1L)).containsSame(first);
            assertThat(sut.findAllByProjectIds(null)).isEmpty();
            assertThat(sut.findAllByProjectIds(List.of())).isEmpty();
            assertThat(sut.findAllByProjectIds(Set.of(1L))).containsEntry(1L, first);
            assertThat(sut.save(first)).isSameAs(first);
            assertThat(sut.saveAll(List.of(first))).containsExactly(first);
            sut.delete(first);
            sut.deleteAllByProjectId(1L);
            then(repository).should().delete(first);
            then(repository).should().deleteAllByProjectId(1L);
        }
    }

    @Nested
    class ApplicationFormPolicyAdapter {
        @Mock ProjectApplicationFormPolicyJpaRepository repository;

        @Test
        @DisplayName("policy batch는 빈 입력을 단축하고 form별 grouping 후 CRUD를 위임한다")
        void batch_and_crud_contracts() {
            ProjectApplicationForm form = applicationForm(10L, project(1L), 100L);
            ProjectApplicationFormPolicy policy = ProjectApplicationFormPolicy.createForParts(
                form, 20L, Set.of(ChallengerPart.WEB));
            given(repository.findAllByApplicationFormId(10L)).willReturn(List.of(policy));
            given(repository.findAllByApplicationFormIdIn(Set.of(10L))).willReturn(List.of(policy));
            given(repository.save(policy)).willReturn(policy);
            given(repository.saveAll(List.of(policy))).willReturn(List.of(policy));
            ProjectApplicationFormPolicyPersistenceAdapter sut =
                new ProjectApplicationFormPolicyPersistenceAdapter(repository);

            assertThat(sut.listByApplicationFormId(10L)).containsExactly(policy);
            assertThat(sut.listByApplicationFormIds(null)).isEmpty();
            assertThat(sut.listByApplicationFormIds(List.of())).isEmpty();
            assertThat(sut.listByApplicationFormIds(Set.of(10L))).containsEntry(10L, List.of(policy));
            assertThat(sut.save(policy)).isSameAs(policy);
            assertThat(sut.saveAll(List.of(policy))).containsExactly(policy);
            sut.delete(policy);
            sut.deleteByFormSectionId(20L);
            sut.deleteAllByApplicationFormId(10L);
            then(repository).should().delete(policy);
            then(repository).should().deleteByFormSectionId(20L);
            then(repository).should().deleteAllByApplicationFormId(10L);
        }
    }

    @Nested
    class ApplicationAdapter {
        @Mock ProjectApplicationJpaRepository jpaRepository;
        @Mock ProjectApplicationQueryRepository queryRepository;

        @Test
        @DisplayName("지원서 조회·검색·저장 port를 상태와 필터를 보존해 위임한다")
        void delegates_application_operations() {
            ProjectApplication application = application(1L);
            Instant now = Instant.parse("2026-01-04T00:00:00Z");
            given(jpaRepository.findById(1L)).willReturn(Optional.of(application));
            given(jpaRepository.existsByAppliedMatchingRound_Id(2L)).willReturn(true);
            given(queryRepository.findByProjectIdAndApplicantMemberIdAndStatus(
                3L, 4L, ProjectApplicationStatus.SUBMITTED)).willReturn(Optional.of(application));
            given(queryRepository.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
                3L, 4L, 2L, ProjectApplicationStatus.DRAFT)).willReturn(Optional.of(application));
            given(queryRepository.findByProjectIdAndApplicantMemberIdAndStatus(
                3L, 4L, ProjectApplicationStatus.DRAFT)).willReturn(Optional.of(application));
            given(queryRepository.existsByRoundAndApplicantAndStatus(
                2L, 4L, ProjectApplicationStatus.SUBMITTED)).willReturn(true);
            given(jpaRepository.save(application)).willReturn(application);
            given(jpaRepository.saveAll(List.of(application))).willReturn(List.of(application));
            given(jpaRepository.findAllByAppliedMatchingRound_Id(2L)).willReturn(List.of(application));
            given(queryRepository.listDecidableByMatchingRoundIdAndProjectId(2L, 3L))
                .willReturn(List.of(application));
            given(queryRepository.findByIdWithDetails(1L)).willReturn(Optional.of(application));
            given(queryRepository.findAllByIdInWithDetails(List.of(1L))).willReturn(List.of(application));
            given(queryRepository.searchMyApplications(
                4L, 5L, MatchingType.PLAN_DESIGN, ProjectApplicationStatus.SUBMITTED))
                .willReturn(List.of(application));
            given(queryRepository.searchProjectApplications(
                3L, 2L, ProjectApplicationStatus.SUBMITTED, now, true))
                .willReturn(List.of(application));
            given(queryRepository.searchProjectApplicationsByProjectIds(
                Set.of(3L), Set.of(3L), 2L, ProjectApplicationStatus.SUBMITTED, now))
                .willReturn(List.of(application));
            ProjectMemberMatchedRoundInfo matched = new ProjectMemberMatchedRoundInfo(
                3L, 4L, 2L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
            given(queryRepository.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
                Set.of(3L), Set.of(4L))).willReturn(List.of(matched));
            given(jpaRepository.findAllByProjectIdAndStatusIn(
                3L, List.of(ProjectApplicationStatus.DRAFT, ProjectApplicationStatus.SUBMITTED)))
                .willReturn(List.of(application));
            ProjectApplicationPersistenceAdapter sut =
                new ProjectApplicationPersistenceAdapter(jpaRepository, queryRepository);

            assertThat(sut.findById(1L)).containsSame(application);
            assertThat(sut.existsByAppliedMatchingRoundId(2L)).isTrue();
            assertThat(sut.findByProjectIdAndApplicantMemberIdAndStatus(
                3L, 4L, ProjectApplicationStatus.SUBMITTED)).containsSame(application);
            assertThat(sut.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
                3L, 4L, 2L, ProjectApplicationStatus.DRAFT)).containsSame(application);
            assertThat(sut.getDraftByProjectAndMember(3L, 4L)).isSameAs(application);
            assertThat(sut.existsByRoundAndApplicantAndStatus(
                2L, 4L, ProjectApplicationStatus.SUBMITTED)).isTrue();
            assertThat(sut.save(application)).isSameAs(application);
            assertThat(sut.saveAll(List.of(application))).containsExactly(application);
            assertThat(sut.listByMatchingRoundId(2L)).containsExactly(application);
            assertThat(sut.listDecidableByMatchingRoundIdAndProjectId(2L, 3L)).containsExactly(application);
            assertThat(sut.findByIdWithDetails(1L)).containsSame(application);
            assertThat(sut.batchGetByIdsWithDetails(null)).isEmpty();
            assertThat(sut.batchGetByIdsWithDetails(List.of())).isEmpty();
            assertThat(sut.batchGetByIdsWithDetails(List.of(1L))).containsExactly(application);
            assertThat(sut.searchMyApplications(
                4L, 5L, MatchingType.PLAN_DESIGN, ProjectApplicationStatus.SUBMITTED))
                .containsExactly(application);
            assertThat(sut.searchProjectApplications(
                3L, 2L, ProjectApplicationStatus.SUBMITTED, now, true)).containsExactly(application);
            assertThat(sut.searchProjectApplicationsByProjectIds(
                Set.of(3L), Set.of(3L), 2L, ProjectApplicationStatus.SUBMITTED, now))
                .containsExactly(application);
            assertThat(sut.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
                Set.of(3L), Set.of(4L))).containsExactly(matched);
            assertThat(sut.listInProgressByProjectId(3L)).containsExactly(application);
        }

        @Test
        @DisplayName("draft와 batch 조회는 누락된 일부 데이터도 fail-closed로 처리한다")
        void rejects_missing_draft_and_partial_batch() {
            ProjectApplication application = application(1L);
            given(queryRepository.findByProjectIdAndApplicantMemberIdAndStatus(
                3L, 404L, ProjectApplicationStatus.DRAFT)).willReturn(Optional.empty());
            given(queryRepository.findAllByIdInWithDetails(List.of(1L, 404L)))
                .willReturn(List.of(application));
            ProjectApplicationPersistenceAdapter sut =
                new ProjectApplicationPersistenceAdapter(jpaRepository, queryRepository);

            assertThatThrownBy(() -> sut.getDraftByProjectAndMember(3L, 404L))
                .isInstanceOf(ProjectDomainException.class);
            assertThatThrownBy(() -> sut.batchGetByIdsWithDetails(List.of(1L, 404L)))
                .isInstanceOf(ProjectDomainException.class);
        }
    }

    private static Project project(Long id) {
        Project value = Project.createDraft(5L, 6L, 7L, 8L, 7L);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private static ProjectApplicationForm applicationForm(Long id, Project project, Long formId) {
        ProjectApplicationForm value = ProjectApplicationForm.create(project, formId);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private static ProjectMatchingRound round(Long id) {
        ProjectMatchingRound value = ProjectMatchingRound.create(
            "매칭", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 6L,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"),
            Instant.parse("2026-01-03T00:00:00Z")
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private static ProjectApplication application(Long id) {
        Project project = project(3L);
        ProjectApplication value = ProjectApplication.create(
            applicationForm(10L, project, 100L), 20L, 4L, round(2L));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
