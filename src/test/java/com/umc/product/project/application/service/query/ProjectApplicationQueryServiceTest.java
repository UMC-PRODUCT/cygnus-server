package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.access.ProjectApplicationAccessScope;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
import com.umc.product.project.application.port.in.query.dto.GetMyProjectApplicationsQuery;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationQueryServiceTest {

    private static final Long REQUESTER_ID = 100L;
    private static final Long GISU_ID = 10L;
    @Mock
    LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    LoadProjectApplicationFormPolicyPort loadProjectApplicationFormPolicyPort;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    GetFormUseCase getFormUseCase;
    @Mock
    GetFormResponseUseCase getFormResponseUseCase;
    @Mock
    ProjectApplicationAccessScopeResolver accessScopeResolver;
    @InjectMocks
    ProjectApplicationQueryService sut;

    @BeforeEach
    void setUpApplicantScope() {
        lenient().when(accessScopeResolver.resolveForApplicant(REQUESTER_ID))
            .thenReturn(new ProjectApplicationAccessScope.OwnerOnly(REQUESTER_ID));
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================================
    //          listMyApplications (본인 지원 내역 자원 조회) 테스트
    //   - 자원(ProjectApplication) 한 종류만 반환한다.
    //   - 화면 카드 합성 / 랜덤 매칭 멤버 합성은 Web Assembler 책임.
    // ============================================================

    @Test
    @DisplayName("listMyApplications_정책_scope가_없으면_외부_조회_없이_빈_목록")
    void 본인_지원서_scope_없으면_빈_리스트() {
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(accessScopeResolver.resolveForApplicant(REQUESTER_ID))
            .willReturn(new ProjectApplicationAccessScope.None());

        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        assertThat(result).isEmpty();
        verify(getChallengerUseCase, never()).findByMemberIdAndGisuId(any(), any());
        verify(loadProjectApplicationPort, never()).searchMyApplications(any(), any(), any(), any());
    }

    @Test
    @DisplayName("listMyApplications_해당_기수_챌린저가_아니면_빈_리스트_반환")
    void 챌린저_아님_빈_리스트() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.empty());

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).isEmpty();
        verify(loadProjectApplicationPort, never())
            .searchMyApplications(any(), any(), any(), any());
    }

    @Test
    @DisplayName("listMyApplications_PLAN_파트는_지원_대상이_아니므로_빈_리스트")
    void PLAN_파트_빈_리스트() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.PLAN)));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).isEmpty();
        verify(loadProjectApplicationPort, never())
            .searchMyApplications(any(), any(), any(), any());
    }

    @Test
    @DisplayName("listMyApplications_ADMIN_파트는_지원_대상이_아니므로_빈_리스트")
    void ADMIN_파트_빈_리스트() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.ADMIN)));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).isEmpty();
        verify(loadProjectApplicationPort, never())
            .searchMyApplications(any(), any(), any(), any());
    }

    @Test
    @DisplayName("listMyApplications_DESIGN_파트는_PLAN_DESIGN_매칭으로_필터링")
    void DESIGN_PLAN_DESIGN_매칭() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.DESIGN)));
        given(loadProjectApplicationPort.searchMyApplications(
            REQUESTER_ID, GISU_ID, MatchingType.PLAN_DESIGN, null))
            .willReturn(List.of());

        // when
        sut.listMyApplications(query);

        // then
        verify(loadProjectApplicationPort)
            .searchMyApplications(REQUESTER_ID, GISU_ID, MatchingType.PLAN_DESIGN, null);
    }

    @Test
    @DisplayName("listMyApplications_WEB_파트는_PLAN_DEVELOPER_매칭으로_필터링")
    void WEB_PLAN_DEVELOPER_매칭() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            REQUESTER_ID, GISU_ID, MatchingType.PLAN_DEVELOPER, null))
            .willReturn(List.of());

        // when
        sut.listMyApplications(query);

        // then
        verify(loadProjectApplicationPort)
            .searchMyApplications(REQUESTER_ID, GISU_ID, MatchingType.PLAN_DEVELOPER, null);
    }

    @Test
    @DisplayName("listMyApplications_status_명시시_해당_상태로_Port_호출")
    void status_명시_전달() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(ProjectApplicationStatus.SUBMITTED);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            REQUESTER_ID, GISU_ID, MatchingType.PLAN_DEVELOPER, ProjectApplicationStatus.SUBMITTED))
            .willReturn(List.of());

        // when
        sut.listMyApplications(query);

        // then
        verify(loadProjectApplicationPort).searchMyApplications(
            REQUESTER_ID, GISU_ID, MatchingType.PLAN_DEVELOPER, ProjectApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("listMyApplications_status_필터가_있어도_decisionDeadline_전이면_결과에서_제외")
    void filteredStatusApplicationBeforeDecisionDeadlineIsExcluded() {
        // given
        Project project = createProject(1L, "프로젝트A", "thumb-1", 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectApplication application = createApplication(
            55L, project, round, ProjectApplicationStatus.APPROVED);

        GetMyProjectApplicationsQuery query = queryOf(ProjectApplicationStatus.APPROVED);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            eq(REQUESTER_ID), eq(GISU_ID), eq(MatchingType.PLAN_DEVELOPER),
            eq(ProjectApplicationStatus.APPROVED)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listMyApplications_status_필터가_있고_decisionDeadline_후면_결과에_포함")
    void filteredStatusApplicationAfterDecisionDeadlineIsIncluded() {
        // given
        Project project = createProject(1L, "프로젝트A", "thumb-1", 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        markDecisionDeadlinePassed(round);
        ProjectApplication application = createApplication(
            55L, project, round, ProjectApplicationStatus.APPROVED);

        GetMyProjectApplicationsQuery query = queryOf(ProjectApplicationStatus.APPROVED);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            eq(REQUESTER_ID), eq(GISU_ID), eq(MatchingType.PLAN_DEVELOPER),
            eq(ProjectApplicationStatus.APPROVED)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ProjectApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("listMyApplications_지원_내역이_없으면_빈_리스트")
    void 지원_내역_없음_빈_리스트() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(any(), any(), any(), any()))
            .willReturn(List.of());

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listMyApplications_decisionDeadline_전이면_지원서_상태를_null_로_반환")
    void masksStatusBeforeDecisionDeadlineInMyApplications() {
        // given
        Project project = createProject(1L, "프로젝트A", "thumb-1", 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectApplication application = createApplication(
            55L, project, round, ProjectApplicationStatus.SUBMITTED);

        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            eq(REQUESTER_ID), eq(GISU_ID), eq(MatchingType.PLAN_DEVELOPER), eq(null)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).hasSize(1);
        ProjectApplicationSummaryInfo info = result.get(0);
        assertThat(info.id()).isEqualTo(55L);
        assertThat(info.projectId()).isEqualTo(1L);
        assertThat(info.matchingRoundId()).isEqualTo(7L);
        assertThat(info.status()).isNull();
        assertThat(info.applicantMemberId()).isEqualTo(REQUESTER_ID);
    }

    @Test
    @DisplayName("listMyApplications_decisionDeadline_후면_상태와_무관하게_상태_반환")
    void statusVisibleAfterDecisionDeadlineRegardlessOfApplicationStatus() {
        // given
        Project project = createProject(1L, "프로젝트A", "thumb-1", 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        markDecisionDeadlinePassed(round);
        ProjectApplication submittedApplication = createApplication(
            55L, project, round, ProjectApplicationStatus.SUBMITTED);
        ProjectApplication approvedApplication = createApplication(
            56L, project, round, ProjectApplicationStatus.APPROVED);
        ProjectApplication rejectedApplication = createApplication(
            57L, project, round, ProjectApplicationStatus.REJECTED);

        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            eq(REQUESTER_ID), eq(GISU_ID), eq(MatchingType.PLAN_DEVELOPER), eq(null)))
            .willReturn(List.of(submittedApplication, approvedApplication, rejectedApplication));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result)
            .extracting(ProjectApplicationSummaryInfo::status)
            .containsExactly(
                ProjectApplicationStatus.SUBMITTED,
                ProjectApplicationStatus.APPROVED,
                ProjectApplicationStatus.REJECTED
            );
    }

    @Test
    @DisplayName("listMyApplications_decisionDeadline_전이면_APPROVED_지원서도_상태_null")
    void approvedStatusHiddenBeforeDecisionDeadline() {
        // given
        Project project = createProject(1L, "프로젝트A", "thumb-1", 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectApplication application = createApplication(
            55L, project, round, ProjectApplicationStatus.APPROVED);

        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challengerOf(ChallengerPart.WEB)));
        given(loadProjectApplicationPort.searchMyApplications(
            eq(REQUESTER_ID), eq(GISU_ID), eq(MatchingType.PLAN_DEVELOPER), eq(null)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.listMyApplications(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isNull();
    }

    // ============================================================
    //          searchByProject (PM/운영진 지원자 목록 조회) 테스트
    // ============================================================

    @Test
    @DisplayName("searchByProject_DRAFT_상태_필터를_사용하면_도메인_예외")
    void searchByProject_DRAFT_필터_금지() {
        // given & when & then -- Query record compact constructor 에서 차단된다
        assertThatThrownBy(() -> SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .projectId(1L)
            .status(ProjectApplicationStatus.DRAFT)
            .build())
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    @DisplayName("searchByProject_권한_scope_가_None_이면_빈_리스트_위장")
    void searchByProject_권한_없음_빈_리스트() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID).projectId(1L).build();

        given(loadProjectPort.getById(1L)).willReturn(project);
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.None());

        // when
        List<ProjectApplicationSummaryInfo> result = sut.searchByProject(query);

        // then
        assertThat(result).isEmpty();
        verify(loadProjectApplicationPort, never())
            .searchProjectApplications(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("searchByProject_지원서가_없으면_빈_리스트")
    void searchByProject_빈_지원서_빈_리스트() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID).projectId(1L).build();

        given(loadProjectPort.getById(1L)).willReturn(project);
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(1L));
        given(loadProjectApplicationPort.searchProjectApplications(eq(1L), isNull(), isNull(), any(), eq(false)))
            .willReturn(List.of());

        // when
        List<ProjectApplicationSummaryInfo> result = sut.searchByProject(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchByProject_엔티티가_있으면_ProjectApplicationSummaryInfo_로_매핑")
    void searchByProject_자원_매핑_정상() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectApplication application = createSubmittedApplication(
            55L, project, round, 200L, ProjectApplicationStatus.APPROVED);

        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID).projectId(1L).build();

        given(loadProjectPort.getById(1L)).willReturn(project);
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(1L));
        given(loadProjectApplicationPort.searchProjectApplications(eq(1L), isNull(), isNull(), any(), eq(false)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.searchByProject(query);

        // then
        assertThat(result).hasSize(1);
        ProjectApplicationSummaryInfo info = result.get(0);
        assertThat(info.id()).isEqualTo(55L);
        assertThat(info.applicantMemberId()).isEqualTo(200L);
        assertThat(info.projectId()).isEqualTo(1L);
        assertThat(info.matchingRoundId()).isEqualTo(7L);
        assertThat(info.status()).isEqualTo(ProjectApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("searchByProject_중앙총괄단_scope면_진행_중_차수_지원서까지_조회")
    void searchByProject_중앙총괄단은_진행중_차수까지_조회() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ReflectionTestUtils.setField(round, "endsAt", java.time.Instant.now().plusSeconds(3_600));
        ProjectApplication application = createSubmittedApplication(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED);

        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID).projectId(1L).build();

        given(loadProjectPort.getById(1L)).willReturn(project);
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(1L, true));
        given(loadProjectApplicationPort.searchProjectApplications(eq(1L), isNull(), isNull(), any(), eq(true)))
            .willReturn(List.of(application));

        // when
        List<ProjectApplicationSummaryInfo> result = sut.searchByProject(query);

        // then
        assertThat(result).hasSize(1);
        verify(loadProjectApplicationPort).searchProjectApplications(eq(1L), isNull(), isNull(), any(), eq(true));
    }

    @Test
    @DisplayName("searchByProject_matchingRoundId_status_필터는_repository_로_그대로_전달")
    void searchByProject_필터_전달() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .projectId(1L)
            .matchingRoundId(7L)
            .status(ProjectApplicationStatus.APPROVED)
            .build();

        given(loadProjectPort.getById(1L)).willReturn(project);
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(1L));
        given(loadProjectApplicationPort.searchProjectApplications(
            eq(1L), eq(7L), eq(ProjectApplicationStatus.APPROVED), any(), eq(false)))
            .willReturn(List.of());

        // when
        sut.searchByProject(query);

        // then
        verify(loadProjectApplicationPort).searchProjectApplications(
            eq(1L), eq(7L), eq(ProjectApplicationStatus.APPROVED), any(), eq(false));
    }

    @Test
    @DisplayName("searchByProjects_요청한_projectId_key를_보존하고_권한_없는_프로젝트와_없는_프로젝트는_빈_리스트")
    void searchByProjects_키_보존_및_빈_리스트() {
        // given
        Project projectA = createProject(1L, "프로젝트A", null, 99L);
        Project projectB = createProject(2L, "프로젝트B", null, 88L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectApplication application = createSubmittedApplication(
            55L, projectA, round, 200L, ProjectApplicationStatus.SUBMITTED);
        SearchProjectApplicationsBatchQuery query = SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .projectIds(List.of(1L, 2L, 3L))
            .build();

        given(loadProjectPort.listByIds(List.of(1L, 2L, 3L)))
            .willReturn(List.of(projectA, projectB));
        given(accessScopeResolver.resolveForProjectApplicantLists(REQUESTER_ID, List.of(projectA, projectB)))
            .willReturn(Map.of(
                1L, new ProjectApplicationAccessScope.ProjectScoped(1L),
                2L, new ProjectApplicationAccessScope.None()
            ));
        given(loadProjectApplicationPort.searchProjectApplicationsByProjectIds(
            eq(Set.of(1L)), eq(Set.of()), isNull(), isNull(), any()))
            .willReturn(List.of(application));

        // when
        Map<Long, List<ProjectApplicationSummaryInfo>> result = sut.searchByProjects(query);

        // then
        assertThat(result.keySet()).containsExactly(1L, 2L, 3L);
        assertThat(result.get(1L)).extracting(ProjectApplicationSummaryInfo::id).containsExactly(55L);
        assertThat(result.get(2L)).isEmpty();
        assertThat(result.get(3L)).isEmpty();
        verify(loadProjectApplicationPort).searchProjectApplicationsByProjectIds(
            eq(Set.of(1L)), eq(Set.of()), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("searchByProjects_ProjectScoped_includeOngoing_true인_프로젝트만_진행중_차수_포함_목록으로_전달")
    void searchByProjects_includeOngoing_프로젝트만_전달() {
        // given
        Project projectA = createProject(1L, "프로젝트A", null, 99L);
        Project projectB = createProject(2L, "프로젝트B", null, 88L);
        SearchProjectApplicationsBatchQuery query = SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .projectIds(List.of(1L, 2L))
            .matchingRoundId(7L)
            .status(ProjectApplicationStatus.APPROVED)
            .build();

        given(loadProjectPort.listByIds(List.of(1L, 2L)))
            .willReturn(List.of(projectA, projectB));
        given(accessScopeResolver.resolveForProjectApplicantLists(REQUESTER_ID, List.of(projectA, projectB)))
            .willReturn(Map.of(
                1L, new ProjectApplicationAccessScope.ProjectScoped(1L),
                2L, new ProjectApplicationAccessScope.ProjectScoped(2L, true)
            ));
        given(loadProjectApplicationPort.searchProjectApplicationsByProjectIds(
            eq(Set.of(1L, 2L)),
            eq(Set.of(2L)),
            eq(7L),
            eq(ProjectApplicationStatus.APPROVED),
            any()
        )).willReturn(List.of());

        // when
        sut.searchByProjects(query);

        // then
        verify(loadProjectApplicationPort).searchProjectApplicationsByProjectIds(
            eq(Set.of(1L, 2L)),
            eq(Set.of(2L)),
            eq(7L),
            eq(ProjectApplicationStatus.APPROVED),
            any()
        );
    }

    @Test
    @DisplayName("searchByProjects_DRAFT_상태_필터를_사용하면_도메인_예외")
    void searchByProjects_DRAFT_필터_금지() {
        assertThatThrownBy(() -> SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .projectIds(List.of(1L))
            .status(ProjectApplicationStatus.DRAFT)
            .build())
            .isInstanceOf(ProjectDomainException.class);
    }

    // ============================================================
    //                getDetail (지원서 단건 상세 조회) 테스트
    // ============================================================

    @Test
    @DisplayName("getDetail_application_미존재시_PROJECT_APPLICATION_NOT_FOUND")
    void getDetail_미존재() {
        // given
        GetProjectApplicationDetailQuery query = detailQuery(1L, 999L);
        given(loadProjectApplicationPort.findByIdWithDetails(999L))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.getDetail(query))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("getDetail_application_은_존재하나_path_projectId_와_form_project_id_가_다르면_NOT_FOUND_위장")
    void getDetail_정합성_위반() {
        // given - application 의 form.project.id 는 1L 인데 path 는 2L
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createSubmittedApplication(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED);

        GetProjectApplicationDetailQuery query = detailQuery(2L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> sut.getDetail(query))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        verify(getChallengerUseCase, never()).findByMemberIdAndGisuId(any(), any());
    }

    @Test
    @DisplayName("getDetail_지원_진행_중인_차수면_APPLICANTS_NOT_VIEWABLE_로_차단")
    void getDetail_지원_진행_중_차단() {
        // given - endsAt 이 미래라 아직 지원 기간 중
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ReflectionTestUtils.setField(round, "endsAt", java.time.Instant.now().plusSeconds(3_600));
        ProjectApplication application = createSubmittedApplication(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> sut.getDetail(query))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode",
                ProjectErrorCode.PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE);
        verify(getChallengerUseCase, never()).findByMemberIdAndGisuId(any(), any());
    }

    @Test
    @DisplayName("getDetail_중앙총괄단_scope면_지원_진행_중인_차수도_조회")
    void getDetail_중앙총괄단은_지원_진행_중에도_조회() {
        // given - endsAt 이 미래라 아직 지원 기간 중이지만 중앙 총괄단 scope 로 통과
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ReflectionTestUtils.setField(round, "endsAt", java.time.Instant.now().plusSeconds(3_600));
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(accessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(1L, true));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of())
                .build()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet())).willReturn(
            FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("getDetail_지원자가_해당_기수_챌린저가_아니면_NOT_FOUND_위장")
    void getDetail_챌린저_누락() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createSubmittedApplication(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.getDetail(query))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        verify(getFormUseCase, never()).getFormWithStructureByQuestionIds(any(), any());
    }

    @Test
    @DisplayName("getDetail_정상_조립_applicantPart_status_매핑_및_answers_files_매핑_확인")
    void getDetail_정상_조립() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));

        FormWithStructureInfo formStructure = FormWithStructureInfo.builder()
            .formId(7L)
            .title("지원폼")
            .description(null)
            .sections(List.of())
            .build();
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());

        AnswerInfo textAnswer = AnswerInfo.builder()
            .id(501L)
            .formResponseId(123L)
            .questionId(11L)
            .answeredAsType(QuestionType.SHORT_TEXT)
            .textValue("이방토/이예원")
            .selectedOptions(List.of())
            .fileIds(null)
            .times(null)
            .build();
        AnswerInfo fileAnswer = AnswerInfo.builder()
            .id(504L)
            .formResponseId(123L)
            .questionId(16L)
            .answeredAsType(QuestionType.PORTFOLIO)
            .textValue(null)
            .selectedOptions(List.of())
            .fileIds(java.util.Set.of("f-abc"))
            .times(null)
            .build();
        FormResponseWithAnswersInfo formResponseWithAnswers = FormResponseWithAnswersInfo.builder()
            .id(123L)
            .formId(7L)
            .respondentMemberId(200L)
            .status(FormResponseStatus.SUBMITTED)
            .submittedAt(java.time.Instant.parse("2026-04-22T01:30:00Z"))
            .submittedIp("127.0.0.1")
            .lastSavedAt(java.time.Instant.parse("2026-04-22T01:30:00Z"))
            .createdAt(java.time.Instant.parse("2026-04-22T01:00:00Z"))
            .updatedAt(java.time.Instant.parse("2026-04-22T01:30:00Z"))
            .answers(List.of(textAnswer, fileAnswer))
            .build();
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(formResponseWithAnswers));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet()))
            .willReturn(formStructure);

        FileInfo fileInfo = new FileInfo(
            "f-abc", "포트폴리오.pdf", FileCategory.PORTFOLIO,
            "application/pdf", 1024L, "https://cdn/f-abc",
            true, 200L, java.time.Instant.parse("2026-04-22T01:00:00Z"));
        given(getFileUseCase.findAllByIds(anyList()))
            .willReturn(Map.of("f-abc", fileInfo));

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
        assertThat(result.applicantMemberId()).isEqualTo(200L);
        assertThat(result.applicantPart()).isEqualTo(ChallengerPart.DESIGN);
        assertThat(result.matchingRoundId()).isEqualTo(7L);
        assertThat(result.matchingRoundType()).isEqualTo(MatchingType.PLAN_DESIGN);
        assertThat(result.matchingRoundPhase()).isEqualTo(MatchingPhase.FIRST);
        assertThat(result.status()).isEqualTo(ProjectApplicationViewStatus.SUBMITTED);
        assertThat(result.formResponse().id()).isEqualTo(123L);
        assertThat(result.answersByQuestionId()).containsOnlyKeys(11L, 16L);
        assertThat(result.filesByFileId()).containsKeys("f-abc");
        assertThat(result.filesByFileId().get("f-abc").originalFileName()).isEqualTo("포트폴리오.pdf");
    }

    // DRAFT 본인 한정 위반(타인 차단) 케이스는 컨트롤러의 @CheckAccess (L2 evaluator.canRead) 가
    // 처리하므로 service 단위 테스트에서는 다루지 않는다. 해당 분기 검증은
    // ProjectApplicationPermissionEvaluatorTest 의 READ는_DRAFT_지원서를_PO여도_거부 등에서 보장.

    @Test
    @DisplayName("getDetail_DRAFT_상태이지만_지원자_본인_호출이면_정상_조회")
    void getDetail_DRAFT_본인_허용() {
        // given - applicantMemberId 와 requesterMemberId 가 동일
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.DRAFT, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L, 200L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.DRAFT)
                .answers(List.of())
                .build()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet()))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
        assertThat(result.status()).isEqualTo(ProjectApplicationViewStatus.DRAFT);
    }

    @Test
    @DisplayName("getDetail_지원_진행_중이어도_지원자_본인_호출이면_정상_조회")
    void getDetail_지원_진행_중_본인_허용() {
        // given - endsAt 이 미래(지원 진행 중)이지만 본인(200==200) 호출
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ReflectionTestUtils.setField(round, "endsAt", java.time.Instant.now().plusSeconds(3_600));
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L, 200L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of())
                .build()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet()))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("getDetail_지원자_본인_호출은_지원서_내용을_보여주되_decisionDeadline_전_상태는_null")
    void selfDetailMasksStatusBeforeDecisionDeadline() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.APPROVED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L, 200L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of())
                .build()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet()))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
        assertThat(result.status()).isNull();
        assertThat(result.formResponse().id()).isEqualTo(123L);
    }

    @Test
    @DisplayName("getDetail_지원자_본인_호출은_decisionDeadline_후_상태를_반환")
    void selfDetailReturnsStatusAfterDecisionDeadline() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        markDecisionDeadlinePassed(round);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.REJECTED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L, 200L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of())
                .build()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), anySet()))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.applicationId()).isEqualTo(55L);
        assertThat(result.status()).isEqualTo(ProjectApplicationViewStatus.REJECTED);
        assertThat(result.formResponse().id()).isEqualTo(123L);
    }

    @Test
    @DisplayName("getDetail_formResponseId_가_dangling_이면_NOT_FOUND_위장")
    void getDetail_dangling_formResponse() {
        // given
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 999L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponseWithAnswers(999L))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.getDetail(query))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("getDetail_질문_fork_후에도_구_질문의_답변이_answersByQuestionId에_포함됨")
    void getDetail_fork된_구_질문_답변_노출() {
        // given
        // 질문 A(id=10)가 fork되어 비활성화됨. 지원자는 questionId=10에 답변을 제출한 상태.
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication application = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 123L);

        GetProjectApplicationDetailQuery query = detailQuery(1L, 55L);
        given(loadProjectApplicationPort.findByIdWithDetails(55L))
            .willReturn(Optional.of(application));
        given(getChallengerUseCase.findByMemberIdAndGisuId(200L, GISU_ID))
            .willReturn(Optional.of(challengerInfoOf(200L, ChallengerPart.DESIGN)));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(any()))
            .willReturn(List.of());

        AnswerInfo forkedQuestionAnswer = AnswerInfo.builder()
            .id(501L)
            .formResponseId(123L)
            .questionId(10L)  // fork로 비활성화된 구 질문 ID
            .answeredAsType(QuestionType.SHORT_TEXT)
            .textValue("답변 내용")
            .selectedOptions(List.of())
            .fileIds(null)
            .times(null)
            .build();
        given(getFormResponseUseCase.findResponseWithAnswers(123L))
            .willReturn(Optional.of(FormResponseWithAnswersInfo.builder()
                .id(123L).formId(7L).respondentMemberId(200L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of(forkedQuestionAnswer))
                .build()));
        // answeredQuestionIds = {10L} 로 getFormWithStructureByQuestionIds 가 호출되어야 함
        given(getFormUseCase.getFormWithStructureByQuestionIds(eq(7L), eq(java.util.Set.of(10L))))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        ProjectApplicationDetailInfo result = sut.getDetail(query);

        // then
        assertThat(result.answersByQuestionId()).containsOnlyKeys(10L);
        assertThat(result.answersByQuestionId().get(10L).textValue()).isEqualTo("답변 내용");
        verify(getFormUseCase).getFormWithStructureByQuestionIds(eq(7L), eq(java.util.Set.of(10L)));
    }

    @Test
    @DisplayName("batchGetDetails_applicationId_목록을_batch_port와_batch_facade로_조회")
    void batchGetDetails_batch_조회() {
        // given
        Instant evaluatedAt = Instant.parse("2026-07-13T00:00:00Z");
        SubjectAttributes subject = new SubjectPolicyFacts(
            evaluatedAt, List.of(), List.of(), Map.of()
        ).toSubjectAttributes(REQUESTER_ID, 1L);
        Project project = createProject(1L, "프로젝트A", null, 99L);
        ProjectMatchingRound round = createMatchingRound(
            7L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST);
        ProjectApplication firstApplication = createApplicationWithFormResponse(
            55L, project, round, 200L, ProjectApplicationStatus.SUBMITTED, 123L);
        ProjectApplication secondApplication = createApplicationWithFormResponse(
            56L, project, round, 201L, ProjectApplicationStatus.SUBMITTED, 124L);

        given(loadProjectApplicationPort.batchGetByIdsWithDetails(Set.of(55L, 56L)))
            .willReturn(List.of(firstApplication, secondApplication));
        given(accessScopeResolver.resolveForProjectApplicantLists(eq(subject), any()))
            .willReturn(Map.of(1L, new ProjectApplicationAccessScope.ProjectScoped(1L, true)));
        given(getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(200L, 201L), GISU_ID))
            .willReturn(Map.of(
                200L, challengerInfoOf(200L, ChallengerPart.DESIGN),
                201L, challengerInfoOf(201L, ChallengerPart.WEB)
            ));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormIds(Set.of(33L)))
            .willReturn(Map.of(33L, List.of()));
        given(getFormResponseUseCase.findResponsesWithAnswers(Set.of(123L, 124L)))
            .willReturn(Map.of(
                123L, FormResponseWithAnswersInfo.builder()
                    .id(123L).formId(7L).respondentMemberId(200L)
                    .status(FormResponseStatus.SUBMITTED)
                    .answers(List.of())
                    .build(),
                124L, FormResponseWithAnswersInfo.builder()
                    .id(124L).formId(7L).respondentMemberId(201L)
                    .status(FormResponseStatus.SUBMITTED)
                    .answers(List.of())
                    .build()
            ));
        given(getFormUseCase.getFormWithStructureByQuestionIds(7L, Set.of()))
            .willReturn(FormWithStructureInfo.builder().formId(7L).sections(List.of()).build());

        // when
        Map<Long, ProjectApplicationDetailInfo> result = sut.batchGetDetails(
            List.of(detailQuery(1L, 55L), detailQuery(1L, 56L)),
            subject
        );

        // then
        assertThat(result).containsOnlyKeys(55L, 56L);
        assertThat(result.get(55L).applicantPart()).isEqualTo(ChallengerPart.DESIGN);
        assertThat(result.get(56L).applicantPart()).isEqualTo(ChallengerPart.WEB);
        verify(loadProjectApplicationPort, never()).findByIdWithDetails(any());
        verify(getFormResponseUseCase, never()).findResponseWithAnswers(any());
        verify(accessScopeResolver, never()).resolveForProjectApplicantLists(eq(REQUESTER_ID), any());
    }

    // ============================================================
    //                      Helper Methods
    // ============================================================

    private GetMyProjectApplicationsQuery queryOf(ProjectApplicationStatus status) {
        return GetMyProjectApplicationsQuery.builder()
            .requesterMemberId(REQUESTER_ID)
            .gisuId(GISU_ID)
            .status(status)
            .build();
    }

    private ChallengerInfo challengerOf(ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(REQUESTER_ID)
            .gisuId(GISU_ID)
            .part(part)
            .build();
    }

    private ChallengerInfo challengerInfoOf(Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(memberId * 10)
            .memberId(memberId)
            .gisuId(GISU_ID)
            .part(part)
            .build();
    }

    private Project createProject(Long id, String name, String thumbnailFileId, Long ownerMemberId) {
        Project project = newInstance(Project.class);
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "gisuId", GISU_ID);
        ReflectionTestUtils.setField(project, "chapterId", 1L);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "name", name);
        ReflectionTestUtils.setField(project, "thumbnailFileId", thumbnailFileId);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerMemberId);
        return project;
    }

    private ProjectMatchingRound createMatchingRound(Long id, MatchingType type, MatchingPhase phase) {
        // getDetail 의 조회 가능 시점 검증을 통과하도록 지원 종료(endsAt)를 과거로 둔다.
        ProjectMatchingRound round = newInstance(ProjectMatchingRound.class);
        ReflectionTestUtils.setField(round, "id", id);
        ReflectionTestUtils.setField(round, "type", type);
        ReflectionTestUtils.setField(round, "phase", phase);
        ReflectionTestUtils.setField(round, "endsAt", java.time.Instant.now().minusSeconds(3_600));
        ReflectionTestUtils.setField(round, "decisionDeadline", java.time.Instant.now().plusSeconds(3_600));
        return round;
    }

    private void markDecisionDeadlinePassed(ProjectMatchingRound round) {
        ReflectionTestUtils.setField(round, "decisionDeadline", java.time.Instant.now().minusSeconds(3_600));
    }

    private ProjectApplication createApplication(
        Long id, Project project, ProjectMatchingRound round, ProjectApplicationStatus status
    ) {
        ProjectApplicationForm form = newInstance(ProjectApplicationForm.class);
        ReflectionTestUtils.setField(form, "project", project);

        ProjectApplication application = newInstance(ProjectApplication.class);
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "applicationForm", form);
        ReflectionTestUtils.setField(application, "appliedMatchingRound", round);
        ReflectionTestUtils.setField(application, "applicantMemberId", REQUESTER_ID);
        ReflectionTestUtils.setField(application, "status", status);
        return application;
    }

    private ProjectApplication createSubmittedApplication(
        Long id, Project project, ProjectMatchingRound round,
        Long applicantMemberId, ProjectApplicationStatus status
    ) {
        ProjectApplicationForm form = newInstance(ProjectApplicationForm.class);
        ReflectionTestUtils.setField(form, "project", project);

        ProjectApplication application = newInstance(ProjectApplication.class);
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "applicationForm", form);
        ReflectionTestUtils.setField(application, "appliedMatchingRound", round);
        ReflectionTestUtils.setField(application, "applicantMemberId", applicantMemberId);
        ReflectionTestUtils.setField(application, "status", status);
        ReflectionTestUtils.setField(application, "submittedAt", java.time.Instant.parse("2026-04-22T01:30:00Z"));
        if (status == ProjectApplicationStatus.APPROVED || status == ProjectApplicationStatus.REJECTED) {
            ReflectionTestUtils.setField(application, "statusChangedAt",
                java.time.Instant.parse("2026-04-22T03:33:00Z"));
        }
        return application;
    }

    private GetProjectApplicationDetailQuery detailQuery(Long projectId, Long applicationId) {
        return detailQuery(projectId, applicationId, REQUESTER_ID);
    }

    private GetProjectApplicationDetailQuery detailQuery(Long projectId, Long applicationId, Long requesterMemberId) {
        return GetProjectApplicationDetailQuery.builder()
            .projectId(projectId)
            .applicationId(applicationId)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private ProjectApplication createApplicationWithFormResponse(
        Long id, Project project, ProjectMatchingRound round,
        Long applicantMemberId, ProjectApplicationStatus status, Long formResponseId
    ) {
        ProjectApplicationForm form = newInstance(ProjectApplicationForm.class);
        ReflectionTestUtils.setField(form, "id", 33L);
        ReflectionTestUtils.setField(form, "project", project);
        ReflectionTestUtils.setField(form, "formId", 7L);

        ProjectApplication application = newInstance(ProjectApplication.class);
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "applicationForm", form);
        ReflectionTestUtils.setField(application, "appliedMatchingRound", round);
        ReflectionTestUtils.setField(application, "applicantMemberId", applicantMemberId);
        ReflectionTestUtils.setField(application, "status", status);
        ReflectionTestUtils.setField(application, "formResponseId", formResponseId);
        ReflectionTestUtils.setField(application, "submittedAt", java.time.Instant.parse("2026-04-22T01:30:00Z"));
        return application;
    }
}
