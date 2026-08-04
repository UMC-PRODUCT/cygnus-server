package com.umc.product.project.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.access.ProjectAccessScope.All;
import com.umc.product.project.application.access.ProjectAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectAccessScope.None;
import com.umc.product.project.application.access.ProjectAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectAccessScope.PublicOnly;
import com.umc.product.project.application.access.ProjectAccessScope.WithOwnerIncluded;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectAccessScopeResolverTest {

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    GetChapterUseCase getChapterUseCase;

    @InjectMocks
    ProjectAccessScopeResolver sut;

    // --- resolveForPublicSearch ---

    @Test
    void publicSearch_은_총괄단이면_All() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, gisuId)
        ));
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);

        ProjectAccessScope scope = sut.resolveForPublicSearch(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(All.class);
        assertThat(((All) scope).visibleStatuses()).containsExactlyInAnyOrderElementsOf(requested);
    }

    @Test
    void publicSearch_은_지부장이면_All() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, gisuId)
        ));
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);

        ProjectAccessScope scope = sut.resolveForPublicSearch(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(All.class);
    }

    @Test
    void publicSearch_은_일반_챌린저면_PublicOnly() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of());

        ProjectAccessScope scope = sut.resolveForPublicSearch(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(PublicOnly.class);
    }

    @Test
    void publicSearch_은_학교_회장단이면_PublicOnly() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 7L, gisuId)
        ));

        ProjectAccessScope scope = sut.resolveForPublicSearch(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(PublicOnly.class);
    }

    @Test
    void publicSearch_은_타_기수_지부장이면_PublicOnly() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, 2L)
        ));

        ProjectAccessScope scope = sut.resolveForPublicSearch(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(PublicOnly.class);
    }

    @Test
    void publicSearch_은_운영진이_DRAFT_요청하면_거부() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, gisuId)
        ));

        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.DRAFT, ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                sut.resolveForPublicSearch(memberId, gisuId, requested))
            .isInstanceOf(com.umc.product.project.domain.exception.ProjectDomainException.class);
    }

    @Test
    void publicSearch_은_일반_챌린저가_PR_요청하면_거부() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of());

        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                sut.resolveForPublicSearch(memberId, gisuId, requested))
            .isInstanceOf(com.umc.product.project.domain.exception.ProjectDomainException.class);
    }

    @Test
    void publicSearch_은_비회원이면_PublicOnly() {
        Long gisuId = 1L;

        ProjectAccessScope scope = sut.resolveForPublicSearch(
            null, gisuId, Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED));

        assertThat(scope).isInstanceOf(PublicOnly.class);
    }

    @Test
    void publicSearch_은_비회원이_비공개_status_요청하면_거부() {
        Long gisuId = 1L;

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                sut.resolveForPublicSearch(null, gisuId, Set.of(ProjectStatus.PENDING_REVIEW)))
            .isInstanceOf(com.umc.product.project.domain.exception.ProjectDomainException.class);
    }

    // --- resolveForManagement ---

    @Test
    void management_은_총괄단이면_All() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, gisuId)
        ));
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);

        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(All.class);
    }

    @Test
    void management_은_지부장이면_ChapterScoped() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, gisuId)
        ));

        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(ChapterScoped.class);
        assertThat(((ChapterScoped) scope).chapterId()).isEqualTo(5L);
    }

    @Test
    void management_은_학교_회장이면_본인_학교가_속한_지부의_ChapterScoped() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 7L, gisuId)
        ));
        given(getChapterUseCase.byGisuAndSchool(gisuId, 7L))
            .willReturn(new ChapterInfo(5L, "테스트 지부"));

        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(ChapterScoped.class);
        assertThat(((ChapterScoped) scope).chapterId()).isEqualTo(5L);
    }

    @Test
    void management_은_학교_부회장도_본인_학교가_속한_지부의_ChapterScoped() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 7L, gisuId)
        ));
        given(getChapterUseCase.byGisuAndSchool(gisuId, 7L))
            .willReturn(new ChapterInfo(5L, "테스트 지부"));

        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(ChapterScoped.class);
        assertThat(((ChapterScoped) scope).chapterId()).isEqualTo(5L);
    }

    @Test
    void management_은_PM_챌린저면_OwnerOnly() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of());
        given(loadProjectPort.existsByOwnerAndGisu(memberId, gisuId)).willReturn(true);

        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED, ProjectStatus.ABORTED);
        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(OwnerOnly.class);
        assertThat(((OwnerOnly) scope).memberId()).isEqualTo(memberId);
        // PO 본인은 DRAFT 도 노출 — 요청 status 에 DRAFT union
        assertThat(((OwnerOnly) scope).visibleStatuses()).contains(ProjectStatus.DRAFT);
        assertThat(((OwnerOnly) scope).visibleStatuses())
            .containsAll(requested);
    }

    @Test
    void management_은_운영진_분기는_DRAFT_제외() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, gisuId)
        ));

        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED, ProjectStatus.ABORTED);
        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(ChapterScoped.class);
        assertThat(((ChapterScoped) scope).visibleStatuses())
            .doesNotContain(ProjectStatus.DRAFT);
    }

    @Test
    void management_은_상위_권한자라도_PO_프로젝트가_있으면_본인_PO_프로젝트를_추가_포함한다() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, gisuId)
        ));
        given(loadProjectPort.existsByOwnerAndGisu(memberId, gisuId)).willReturn(true);

        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, requested);

        assertThat(scope).isInstanceOf(WithOwnerIncluded.class);
        WithOwnerIncluded withOwner = (WithOwnerIncluded) scope;
        assertThat(withOwner.baseScope()).isInstanceOf(ChapterScoped.class);
        assertThat(withOwner.ownerMemberId()).isEqualTo(memberId);
        assertThat(withOwner.ownerVisibleStatuses())
            .contains(ProjectStatus.DRAFT)
            .containsAll(requested);
    }

    @Test
    void management_은_관리_대상_없으면_None() {
        Long memberId = 10L;
        Long gisuId = 1L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of());
        given(loadProjectPort.existsByOwnerAndGisu(memberId, gisuId)).willReturn(false);

        ProjectAccessScope scope = sut.resolveForManagement(memberId, gisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    void management_은_타_기수_지부장이면_PM_조회_경로로_빠진다() {
        Long memberId = 10L;
        Long currentGisuId = 2L;
        given(getChallengerRoleUseCase.findAllByMemberId(memberId)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, 1L)
        ));
        lenient().when(loadProjectPort.existsByOwnerAndGisu(memberId, currentGisuId)).thenReturn(false);

        ProjectAccessScope scope = sut.resolveForManagement(memberId, currentGisuId, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(scope).isInstanceOf(None.class);
    }

    // --- helpers ---

    private ChallengerRoleInfo roleInfo(ChallengerRoleType type, OrganizationType orgType, Long orgId, Long gisuId) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(1L)
            .roleType(type)
            .organizationType(orgType)
            .organizationId(orgId)
            .responsiblePart(null)
            .gisuId(gisuId)
            .build();
    }
}
