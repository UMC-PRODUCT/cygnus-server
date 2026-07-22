package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.access.ProjectAccessScope;
import com.umc.product.project.application.access.ProjectAccessScopeResolver;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.SearchManagedProjectQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    ProjectAccessScopeResolver scopeResolver;

    @InjectMocks
    ProjectQueryService sut;

    @Test
    void findAllByIds_빈값_누락_중복파일과_기본집계를_한번에_처리한다() {
        assertThat(sut.findAllByIds(List.of())).isEmpty();
        given(loadProjectPort.listByIds(List.of(999L))).willReturn(List.of());
        assertThat(sut.findAllByIds(List.of(999L))).isEmpty();

        Project first = createProject(1L, ProjectStatus.IN_PROGRESS);
        Project second = createProject(2L, ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(first, "logoFileId", "logo-1");
        ReflectionTestUtils.setField(second, "thumbnailFileId", "thumb-1");
        ReflectionTestUtils.setField(second, "logoFileId", null);
        given(loadProjectPort.listByIds(List.of(1L, 2L))).willReturn(List.of(first, second));
        given(loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(
            Set.of(1L, 2L), ChallengerPart.PLAN)).willReturn(Map.of(
                1L, List.of(createProjectMember(10L), createProjectMember(20L))));
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(Set.of(1L, 2L)))
            .willReturn(Map.of(1L, List.of(createPartQuota(1L, ChallengerPart.WEB, 3))));
        given(loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(Set.of(1L, 2L)))
            .willReturn(Map.of(1L, Map.of()));
        given(getFileUseCase.getFileLinks(List.of("thumb-1", "logo-1")))
            .willReturn(Map.of("thumb-1", "thumb-url", "logo-1", "logo-url"));

        Map<Long, ProjectInfo> result = sut.findAllByIds(List.of(1L, 2L));

        assertThat(result.keySet()).containsExactly(1L, 2L);
        assertThat(result.get(1L).coProductOwnerMemberIds()).containsExactly(20L);
        assertThat(result.get(1L).partQuotas().get(0).currentCount()).isZero();
        assertThat(result.get(1L).logoImageUrl()).isEqualTo("logo-url");
    }

    @Test
    void 단건_파일은_logo만_있거나_모두_없어도_안전하게_조립한다() {
        Project logoOnly = createProject(1L, ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(logoOnly, "thumbnailFileId", null);
        ReflectionTestUtils.setField(logoOnly, "logoFileId", "logo-1");
        given(loadProjectPort.getById(1L)).willReturn(logoOnly);
        given(loadProjectMemberPort.listByProjectIdAndPart(1L, ChallengerPart.PLAN)).willReturn(List.of());
        given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(List.of());
        given(getFileUseCase.getFileLinks(List.of("logo-1"))).willReturn(Map.of("logo-1", "logo-url"));

        assertThat(sut.getById(1L).logoImageUrl()).isEqualTo("logo-url");

        Project noFiles = createProject(2L, ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(noFiles, "thumbnailFileId", null);
        ReflectionTestUtils.setField(noFiles, "logoFileId", null);
        given(loadProjectPort.getById(2L)).willReturn(noFiles);
        given(loadProjectMemberPort.listByProjectIdAndPart(2L, ChallengerPart.PLAN)).willReturn(List.of());
        given(loadProjectPartQuotaPort.listByProjectId(2L)).willReturn(List.of());

        assertThat(sut.getById(2L).thumbnailImageUrl()).isNull();
    }

    @Test
    void batch_프로젝트에_파일이_전혀_없으면_storage를_호출하지_않는다() {
        Project noFiles = createProject(3L, ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(noFiles, "thumbnailFileId", null);
        ReflectionTestUtils.setField(noFiles, "logoFileId", null);
        given(loadProjectPort.listByIds(List.of(3L))).willReturn(List.of(noFiles));
        given(loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(
            Set.of(3L), ChallengerPart.PLAN)).willReturn(Map.of());
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(Set.of(3L))).willReturn(Map.of());
        given(loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(Set.of(3L))).willReturn(Map.of());

        assertThat(sut.findAllByIds(List.of(3L)).get(3L).thumbnailImageUrl()).isNull();
        verify(getFileUseCase, never()).getFileLinks(any());
    }

    @Test
    void 검색_scope의_모든_형태를_query로_변환한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forAdmin(
            1L, null, null, null, null, null, List.of(ProjectStatus.DRAFT), pageable);
        Set<ProjectStatus> statuses = Set.of(ProjectStatus.DRAFT, ProjectStatus.IN_PROGRESS);
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(scopeResolver.resolveForPublicSearch(any(), any(), anySet())).willReturn(
            new ProjectAccessScope.All(statuses),
            new ProjectAccessScope.ChapterScoped(10L, statuses),
            new ProjectAccessScope.OwnerOnly(20L, statuses),
            new ProjectAccessScope.PublicOnly(),
            new ProjectAccessScope.None(),
            new ProjectAccessScope.WithOwnerIncluded(new ProjectAccessScope.All(statuses), 30L, statuses),
            new ProjectAccessScope.WithOwnerIncluded(new ProjectAccessScope.OwnerOnly(20L, statuses), 30L, statuses),
            new ProjectAccessScope.WithOwnerIncluded(new ProjectAccessScope.PublicOnly(), 30L, statuses),
            new ProjectAccessScope.WithOwnerIncluded(new ProjectAccessScope.None(), 30L, statuses),
            new ProjectAccessScope.WithOwnerIncluded(
                new ProjectAccessScope.WithOwnerIncluded(new ProjectAccessScope.All(statuses), 31L, statuses),
                30L,
                statuses)
        );

        for (int i = 0; i < 10; i++) {
            assertThat(sut.search(query, 99L)).isEmpty();
        }
    }

    @Test
    void getById_프로젝트_상세_조회_성공() {
        // given
        Project project = createProject(1L, ProjectStatus.IN_PROGRESS);
        given(loadProjectPort.getById(1L)).willReturn(project);
        given(loadProjectMemberPort.listByProjectIdAndPart(1L, ChallengerPart.PLAN))
            .willReturn(List.of());
        given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(List.of());
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        // when
        ProjectInfo result = sut.getById(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("테스트 프로젝트");
        assertThat(result.thumbnailImageUrl()).isEqualTo("https://cdn.example.com/thumb-1");
        assertThat(result.coProductOwnerMemberIds()).isEmpty();
    }

    @Test
    void getById_존재하지_않으면_예외() {
        // given
        given(loadProjectPort.getById(999L))
            .willThrow(new ProjectDomainException(
                com.umc.product.project.domain.exception.ProjectErrorCode.PROJECT_NOT_FOUND));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.getById(999L))
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    void getById_coPM과_partQuota_포함_조회() {
        // given
        Project project = createProject(1L, ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", 10L);

        given(loadProjectPort.getById(1L)).willReturn(project);

        var coPm = createProjectMember(20L);
        given(loadProjectMemberPort.listByProjectIdAndPart(1L, ChallengerPart.PLAN))
            .willReturn(List.of(coPm));

        var quota = createPartQuota(1L, ChallengerPart.WEB, 3);
        given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(List.of(quota));
        given(loadProjectMemberPort.countByProjectIdGroupByPart(1L))
            .willReturn(Map.of(ChallengerPart.WEB, 2L));
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        // when
        ProjectInfo result = sut.getById(1L);

        // then
        assertThat(result.coProductOwnerMemberIds()).containsExactly(20L);
        assertThat(result.partQuotas()).hasSize(1);
        assertThat(result.partQuotas().get(0).part()).isEqualTo(ChallengerPart.WEB);
        assertThat(result.partQuotas().get(0).quota()).isEqualTo(3);
        assertThat(result.partQuotas().get(0).currentCount()).isEqualTo(2);
    }

    @Test
    void findDraftByCreatorAndGisu_DRAFT_프로젝트_반환() {
        // given
        Project project = createProject(1L, ProjectStatus.DRAFT);
        given(loadProjectPort.findDraftByCreatorAndGisu(10L, 1L))
            .willReturn(Optional.of(project));
        given(loadProjectMemberPort.listByProjectIdAndPart(1L, ChallengerPart.PLAN))
            .willReturn(List.of());
        given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(List.of());
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        // when
        Optional<ProjectInfo> result = sut.findDraftByCreatorAndGisu(10L, 1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Test
    void findDraftByCreatorAndGisu_프로젝트_없으면_empty() {
        // given
        given(loadProjectPort.findDraftByCreatorAndGisu(10L, 1L))
            .willReturn(Optional.empty());

        // when
        Optional<ProjectInfo> result = sut.findDraftByCreatorAndGisu(10L, 1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void search_페이지_결과_변환() {
        // given
        Project project = createProject(1L, ProjectStatus.IN_PROGRESS);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, pageable);

        given(scopeResolver.resolveForPublicSearch(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.PublicOnly());
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(project), pageable, 1));
        given(loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(anySet(), eq(ChallengerPart.PLAN)))
            .willReturn(Map.of());
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anySet()))
            .willReturn(Map.of());
        given(loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(anySet()))
            .willReturn(Map.of());
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        // when
        Page<ProjectInfo> result = sut.search(query, 99L);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    void search_여러_프로젝트_배치_조립() {
        // given
        Project p1 = createProject(1L, ProjectStatus.IN_PROGRESS);
        Project p2 = createProject(2L, ProjectStatus.IN_PROGRESS);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, pageable);

        given(scopeResolver.resolveForPublicSearch(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.PublicOnly());
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));
        given(loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(anySet(), eq(ChallengerPart.PLAN)))
            .willReturn(Map.of(1L, List.of(createProjectMember(20L))));
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anySet()))
            .willReturn(Map.of(2L, List.of(createPartQuota(2L, ChallengerPart.WEB, 3))));
        given(loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(anySet()))
            .willReturn(Map.of(2L, Map.of(ChallengerPart.WEB, 2L)));
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        // when
        Page<ProjectInfo> result = sut.search(query, 99L);

        // then
        assertThat(result.getContent()).hasSize(2);

        ProjectInfo first = result.getContent().get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.coProductOwnerMemberIds()).containsExactly(20L);
        assertThat(first.partQuotas()).isEmpty();

        ProjectInfo second = result.getContent().get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.coProductOwnerMemberIds()).isEmpty();
        assertThat(second.partQuotas()).hasSize(1);
        assertThat(second.partQuotas().get(0).part()).isEqualTo(ChallengerPart.WEB);
        assertThat(second.partQuotas().get(0).currentCount()).isEqualTo(2);
    }

    @Test
    void search_빈_결과() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, pageable);

        given(scopeResolver.resolveForPublicSearch(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.PublicOnly());
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<ProjectInfo> result = sut.search(query, 99L);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(loadProjectMemberPort, never()).listByProjectIdsAndPartGroupedByProjectId(anySet(), any());
    }

    @Test
    void searchManaged_총괄단이면_전체_노출() {
        Project project = createProject(1L, ProjectStatus.PENDING_REVIEW);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).keyword(null).pageable(pageable).build();

        given(scopeResolver.resolveForManagement(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.All(
                java.util.Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS,
                    ProjectStatus.COMPLETED, ProjectStatus.ABORTED)));
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(project), pageable, 1));
        given(loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(anySet(), eq(ChallengerPart.PLAN)))
            .willReturn(Map.of());
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anySet()))
            .willReturn(Map.of());
        given(loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(anySet()))
            .willReturn(Map.of());
        given(getFileUseCase.getFileLinks(List.of("thumb-1")))
            .willReturn(Map.of("thumb-1", "https://cdn.example.com/thumb-1"));

        Page<ProjectInfo> result = sut.searchManaged(query, 99L);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchManaged_일반_챌린저는_빈_페이지() {
        PageRequest pageable = PageRequest.of(0, 20);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).keyword(null).pageable(pageable).build();

        given(scopeResolver.resolveForManagement(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.None());

        Page<ProjectInfo> result = sut.searchManaged(query, 99L);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(loadProjectMemberPort, never()).listByProjectIdsAndPartGroupedByProjectId(anySet(), any());
    }

    @Test
    void searchManaged_상위_scope에서도_본인_PO_프로젝트를_추가_포함하는_query를_전달한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).keyword(null).pageable(pageable).build();
        java.util.Set<ProjectStatus> requested = java.util.Set.of(
            ProjectStatus.PENDING_REVIEW,
            ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED,
            ProjectStatus.ABORTED
        );
        java.util.Set<ProjectStatus> ownerStatuses = java.util.EnumSet.copyOf(requested);
        ownerStatuses.add(ProjectStatus.DRAFT);

        given(scopeResolver.resolveForManagement(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.WithOwnerIncluded(
                new ProjectAccessScope.ChapterScoped(5L, requested),
                99L,
                ownerStatuses
            ));
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchManaged(query, 99L);

        ArgumentCaptor<SearchProjectQuery> captor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        verify(loadProjectPort).search(captor.capture());
        SearchProjectQuery actual = captor.getValue();
        assertThat(actual.chapterId()).isEqualTo(5L);
        assertThat(actual.includedOwnerMemberId()).isEqualTo(99L);
        assertThat(actual.includedOwnerStatuses()).contains(ProjectStatus.DRAFT);
    }

    // ========== Helper Methods ==========

    private Project createProject(Long id, ProjectStatus status) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "gisuId", 1L);
        ReflectionTestUtils.setField(project, "chapterId", 1L);
        ReflectionTestUtils.setField(project, "status", status);
        ReflectionTestUtils.setField(project, "name", "테스트 프로젝트");
        ReflectionTestUtils.setField(project, "description", "테스트 설명");
        ReflectionTestUtils.setField(project, "thumbnailFileId", "thumb-1");
        ReflectionTestUtils.setField(project, "productOwnerMemberId", 10L);
        ReflectionTestUtils.setField(project, "creatorMemberId", 10L);
        return project;
    }

    private com.umc.product.project.domain.ProjectMember createProjectMember(Long memberId) {
        com.umc.product.project.domain.ProjectMember pm;
        try {
            var constructor = com.umc.product.project.domain.ProjectMember.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            pm = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(pm, "memberId", memberId);
        ReflectionTestUtils.setField(pm, "part", ChallengerPart.PLAN);
        return pm;
    }

    private ProjectPartQuota createPartQuota(Long projectId, ChallengerPart part, int quota) {
        ProjectPartQuota pq;
        try {
            var constructor = ProjectPartQuota.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            pq = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(pq, "part", part);
        ReflectionTestUtils.setField(pq, "quota", (long) quota);
        return pq;
    }
}
