package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.umc.product.project.application.access.ScopeClause;
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
        given(scopeResolver.resolveForOwnDraft(10L, 1L))
            .willReturn(new ProjectAccessScope.OwnerOnly(10L, Set.of(ProjectStatus.DRAFT)));
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
        given(scopeResolver.resolveForOwnDraft(10L, 1L))
            .willReturn(new ProjectAccessScope.OwnerOnly(10L, Set.of(ProjectStatus.DRAFT)));
        given(loadProjectPort.findDraftByCreatorAndGisu(10L, 1L))
            .willReturn(Optional.empty());

        // when
        Optional<ProjectInfo> result = sut.findDraftByCreatorAndGisu(10L, 1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findDraftByCreatorAndGisu_정책_scope가_없으면_port를_호출하지_않는다() {
        given(scopeResolver.resolveForOwnDraft(10L, 1L))
            .willReturn(new ProjectAccessScope.None());

        Optional<ProjectInfo> result = sut.findDraftByCreatorAndGisu(10L, 1L);

        assertThat(result).isEmpty();
        verify(loadProjectPort, never()).findDraftByCreatorAndGisu(any(), any());
    }

    @Test
    void search_페이지_결과_변환() {
        // given
        Project project = createProject(1L, ProjectStatus.IN_PROGRESS);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, pageable);

        given(scopeResolver.resolveForPublicSearch(anyLong(), anyLong(), anySet()))
            .willReturn(publicScope());
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

        given(scopeResolver.resolveForPublicSearch(anyLong(), anyLong(), anySet()))
            .willReturn(publicScope());
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

        given(scopeResolver.resolveForPublicSearch(anyLong(), anyLong(), anySet()))
            .willReturn(publicScope());
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
    void search_요청_chapterId를_권한_clause와_AND로_결합한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, 8L, null, null, null, pageable);
        Set<ProjectStatus> publicStatuses = Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED);
        ScopeClause publicClause = ScopeClause.gisu(Set.of(1L), publicStatuses);
        ScopeClause otherChapterClause = ScopeClause.gisu(Set.of(1L), publicStatuses)
            .andChapterIds(Set.of(7L));
        given(scopeResolver.resolveForPublicSearch(anyLong(), anyLong(), anySet()))
            .willReturn(new ProjectAccessScope.Clauses(List.of(publicClause, otherChapterClause)));
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.search(query, 99L);

        ArgumentCaptor<SearchProjectQuery> captor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        verify(loadProjectPort).search(captor.capture());
        assertThat(captor.getValue().scopeClauses()).containsExactly(
            publicClause.andChapterIds(Set.of(8L))
        );
    }

    @Test
    void searchManaged_Clauses를_검색_query에_전달한다() {
        Project project = createProject(1L, ProjectStatus.PENDING_REVIEW);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).keyword(null).pageable(pageable).build();
        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.PENDING_REVIEW,
            ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED,
            ProjectStatus.ABORTED
        );
        ScopeClause allInRequestedGisu = ScopeClause.gisu(Set.of(1L), requested);

        given(scopeResolver.resolveForManagement(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.Clauses(List.of(allInRequestedGisu)));
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
        ArgumentCaptor<SearchProjectQuery> captor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        verify(loadProjectPort).search(captor.capture());
        assertThat(captor.getValue().scopeClauses()).containsExactly(allInRequestedGisu);
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
        verify(loadProjectPort, never()).search(any(SearchProjectQuery.class));
        verify(loadProjectMemberPort, never()).listByProjectIdsAndPartGroupedByProjectId(anySet(), any());
    }

    @Test
    void searchManaged_복수_Clauses를_평탄화하지_않고_OR_분기로_전달한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).keyword(null).pageable(pageable).build();
        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.PENDING_REVIEW,
            ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED,
            ProjectStatus.ABORTED
        );
        Set<ProjectStatus> ownerStatuses = java.util.EnumSet.copyOf(requested);
        ownerStatuses.add(ProjectStatus.DRAFT);
        ScopeClause chapterClause = ScopeClause.gisu(Set.of(1L), requested)
            .andChapterIds(Set.of(5L));
        ScopeClause ownerClause = ScopeClause.gisu(Set.of(1L), ownerStatuses)
            .andOwnerMemberIds(Set.of(99L));

        given(scopeResolver.resolveForManagement(any(), any(), anySet()))
            .willReturn(new ProjectAccessScope.Clauses(List.of(chapterClause, ownerClause)));
        given(loadProjectPort.search(any(SearchProjectQuery.class)))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchManaged(query, 99L);

        ArgumentCaptor<SearchProjectQuery> captor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        verify(loadProjectPort).search(captor.capture());
        SearchProjectQuery actual = captor.getValue();
        assertThat(actual.scopeClauses()).containsExactly(chapterClause, ownerClause);
        assertThat(actual.chapterId()).isNull();
        assertThat(actual.includedOwnerMemberId()).isNull();
    }

    // ========== Helper Methods ==========

    private ProjectAccessScope publicScope() {
        return new ProjectAccessScope.Clauses(List.of(
            ScopeClause.gisu(
                Set.of(1L),
                Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED)
            )
        ));
    }

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
