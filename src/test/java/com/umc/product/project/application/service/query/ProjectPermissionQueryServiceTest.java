package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.project.application.access.ProjectApplicationAccessScope;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionReason;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.policy.ProjectStatisticsAccessPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionQueryServiceTest {

    private static final Long REQUESTER_ID = 10L;
    private static final Long PROJECT_ID = 100L;
    private static final Long SECOND_PROJECT_ID = 101L;
    private static final Long GISU_ID = 1L;
    private static final Long CHAPTER_ID = 7L;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;
    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    @Mock
    LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    ProjectApplicationAccessScopeResolver projectApplicationAccessScopeResolver;
    @Mock
    ProjectStatisticsAccessPolicy projectStatisticsAccessPolicy;

    @InjectMocks
    ProjectPermissionQueryService sut;

    @Test
    void DRAFT_프로젝트는_작성자이고_이름과_지원폼이_있으면_검토_요청이_가능하다() {
        Project project = project(ProjectStatus.DRAFT, REQUESTER_ID, REQUESTER_ID, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenProjectPermission(subject, PermissionType.READ, true);
        givenProjectPermission(subject, PermissionType.EDIT, true);
        givenProjectPermission(subject, PermissionType.DELETE, true);
        givenProjectPermission(subject, PermissionType.MANAGE, false);
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(PROJECT_ID));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, project)).willReturn(true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canRequestReview().allowed()).isTrue();
        assertThat(result.applicationForm().canCreate().allowed()).isFalse();
        assertThat(result.applicationForm().canCreate().reasonCode())
            .isEqualTo(ProjectPermissionReason.NOT_IMPLEMENTED.name());
        assertThat(result.applicationForm().canCreate().reason())
            .isEqualTo("아직은 프로젝트에 여러 개의 폼을 연결하는 것을 허용하지 않아요.");
        assertThat(result.applicationForm().canPublish().reason())
            .isEqualTo("아직은 지원 폼 공개를 별도로 지원하지 않아요.");
        assertThat(result.applicationForm().canDelete().reason())
            .isEqualTo("아직은 지원 폼 삭제를 별도로 지원하지 않아요.");
        // MANAGE 권한이 없으면 상태와 무관하게 완료 불가 — 권한이 상태 검사보다 우선한다.
        assertThat(result.status().canComplete().allowed()).isFalse();
        assertThat(result.status().canComplete().reasonCode())
            .isEqualTo(ProjectPermissionReason.PERMISSION_DENIED.name());
    }

    @Test
    void 권한이_없으면_세부_상태보다_PERMISSION_DENIED가_우선한다() {
        Project project = project(ProjectStatus.DRAFT, 999L, 999L, null);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms();
        givenProjectPermission(subject, PermissionType.EDIT, false);
        givenProjectPermission(subject, PermissionType.READ, false);
        givenProjectPermission(subject, PermissionType.DELETE, false);
        givenProjectPermission(subject, PermissionType.MANAGE, false);
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.None());
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, project)).willReturn(false);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.canEditInfo().allowed()).isFalse();
        assertThat(result.canEditInfo().reasonCode()).isEqualTo(ProjectPermissionReason.PERMISSION_DENIED.name());
        assertThat(result.status().canRequestReview().reasonCode())
            .isEqualTo(ProjectPermissionReason.PERMISSION_DENIED.name());
        assertThat(result.statistics().canRead().reason()).isEqualTo("통계를 조회할 권한이 없어요.");
    }

    @Test
    void PENDING_REVIEW_프로젝트는_운영진이고_지원폼과_정원이_있으면_공개가_가능하다() {
        Project project = project(ProjectStatus.PENDING_REVIEW, 999L, 999L, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenQuotas(project, ChallengerPart.WEB);
        givenProjectPermission(subject, PermissionType.READ, true);
        givenProjectPermission(subject, PermissionType.EDIT, false);
        givenProjectPermission(subject, PermissionType.DELETE, false);
        givenProjectPermission(subject, PermissionType.MANAGE, true);
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(PROJECT_ID));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, project)).willReturn(true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canPublish().allowed()).isTrue();
        assertThat(result.status().canAbort().allowed()).isFalse();
        assertThat(result.status().canAbort().reasonCode()).isEqualTo(ProjectPermissionReason.INVALID_PROJECT_STATUS.name());
        assertThat(result.status().canAbort().reason()).isEqualTo("현재 진행 중인 프로젝트만 중단 시킬 수 있어요.");
        // 운영진이어도 PENDING_REVIEW 는 완료 불가 — 완료는 IN_PROGRESS 에서만 열린다.
        assertThat(result.status().canComplete().allowed()).isFalse();
        assertThat(result.status().canComplete().reasonCode())
            .isEqualTo(ProjectPermissionReason.INVALID_PROJECT_STATUS.name());
        assertThat(result.status().canComplete().reason())
            .isEqualTo("현재 진행 중인 프로젝트만 완료 처리할 수 있어요.");
    }

    @Test
    void IN_PROGRESS_프로젝트는_운영진이면_중단_가능하고_활성_차수가_있으면_지원폼_수정은_불가하다() {
        Project project = project(ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenProjectPermission(subject, PermissionType.READ, true);
        givenProjectPermission(subject, PermissionType.EDIT, true);
        givenProjectPermission(subject, PermissionType.DELETE, true);
        givenProjectPermission(subject, PermissionType.MANAGE, true);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.ProjectScoped(PROJECT_ID));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, project)).willReturn(true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canAbort().allowed()).isTrue();
        // IN_PROGRESS + 운영진(MANAGE) 이면 완료도 가능하다 — 여기서 열린다.
        assertThat(result.status().canComplete().allowed()).isTrue();
        assertThat(result.applicationForm().canEdit().allowed()).isFalse();
        assertThat(result.applicationForm().canEdit().reasonCode())
            .isEqualTo(ProjectPermissionReason.ACTIVE_MATCHING_ROUND_EXISTS.name());
        assertThat(result.canDelete().reason()).isEqualTo("진행 중인 프로젝트는 중단 기능을 이용해주세요.");
    }

    @Test
    void 지원_생성은_모집중이고_폼_정원_오픈차수_챌린저_조건이_모두_맞으면_가능하다() {
        Project project = project(ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenQuotas(project, ChallengerPart.WEB);
        givenProjectPermission(subject, PermissionType.READ, true);
        givenProjectPermission(subject, PermissionType.EDIT, false);
        givenProjectPermission(subject, PermissionType.DELETE, false);
        givenProjectPermission(subject, PermissionType.MANAGE, false);
        givenApplicationPermission(subject, true);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challenger(ChallengerPart.WEB)));
        given(loadProjectMemberPort.existsByGisuAndMember(GISU_ID, REQUESTER_ID)).willReturn(false);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, project))
            .willReturn(new ProjectApplicationAccessScope.None());
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, project)).willReturn(false);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.application().canCreate().allowed()).isTrue();
    }

    @Test
    void 같은_지부와_기수의_프로젝트는_권한_계산용_조회_결과를_캐시한다() {
        Project firstProject = project(PROJECT_ID, GISU_ID, CHAPTER_ID, ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
        Project secondProject = project(
            SECOND_PROJECT_ID,
            GISU_ID,
            CHAPTER_ID,
            ProjectStatus.IN_PROGRESS,
            998L,
            998L,
            "운영 도구"
        );
        ProjectApplicationForm firstForm = ProjectApplicationForm.create(firstProject, 500L);
        ProjectApplicationForm secondForm = ProjectApplicationForm.create(secondProject, 501L);
        SubjectAttributes subject = subject();
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(loadProjectPort.listByIds(List.of(PROJECT_ID, SECOND_PROJECT_ID)))
            .willReturn(List.of(firstProject, secondProject));
        given(loadProjectApplicationFormPort.findAllByProjectIds(anyCollection()))
            .willReturn(Map.of(PROJECT_ID, firstForm, SECOND_PROJECT_ID, secondForm));
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anyCollection()))
            .willReturn(Map.of(
                PROJECT_ID,
                List.of(ProjectPartQuota.create(firstProject, ChallengerPart.WEB, 1L, REQUESTER_ID)),
                SECOND_PROJECT_ID,
                List.of(ProjectPartQuota.create(secondProject, ChallengerPart.WEB, 1L, REQUESTER_ID))
            ));
        givenApplicationCreatePermissions(subject, PROJECT_ID);
        givenApplicationCreatePermissions(subject, SECOND_PROJECT_ID);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challenger(ChallengerPart.WEB)));
        given(loadProjectMemberPort.existsByGisuAndMember(GISU_ID, REQUESTER_ID)).willReturn(false);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, firstProject))
            .willReturn(new ProjectApplicationAccessScope.None());
        given(projectApplicationAccessScopeResolver.resolveForProjectApplicantList(REQUESTER_ID, secondProject))
            .willReturn(new ProjectApplicationAccessScope.None());
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, firstProject)).willReturn(false);
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(REQUESTER_ID, secondProject)).willReturn(false);

        List<ProjectPermissionInfo> results = sut.listByProjectIds(
            REQUESTER_ID,
            List.of(PROJECT_ID, SECOND_PROJECT_ID)
        );

        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(result -> result.application().canCreate().allowed())
            .containsExactly(true, true);
        then(loadProjectMatchingRoundPort).should(times(1)).listOpenAt(eq(CHAPTER_ID), any(Instant.class));
        then(getChallengerUseCase).should(times(1)).findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID);
        then(loadProjectMemberPort).should(times(1)).existsByGisuAndMember(GISU_ID, REQUESTER_ID);
    }

    @Test
    void 프로젝트가_없으면_exists_false와_PROJECT_NOT_FOUND를_반환한다() {
        SubjectAttributes subject = subject();
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(loadProjectPort.listByIds(List.of(PROJECT_ID))).willReturn(List.of());

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.exists()).isFalse();
        assertThat(result.canEditInfo().reasonCode()).isEqualTo(ProjectPermissionReason.PROJECT_NOT_FOUND.name());
        assertThat(result.application().canCreate().reasonCode())
            .isEqualTo(ProjectPermissionReason.PROJECT_NOT_FOUND.name());
    }

    private void givenBase(Project project, SubjectAttributes subject) {
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(loadProjectPort.listByIds(List.of(PROJECT_ID))).willReturn(List.of(project));
        lenient().when(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anyCollection()))
            .thenReturn(Map.of());
        lenient().when(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .thenReturn(List.of());
    }

    private void givenForms(ProjectApplicationForm... forms) {
        Map<Long, ProjectApplicationForm> byProjectId = forms.length == 0
            ? Map.of()
            : Map.of(PROJECT_ID, forms[0]);
        given(loadProjectApplicationFormPort.findAllByProjectIds(anyCollection())).willReturn(byProjectId);
    }

    private void givenQuotas(Project project, ChallengerPart part) {
        given(loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(anyCollection()))
            .willReturn(Map.of(PROJECT_ID, List.of(ProjectPartQuota.create(project, part, 1L, REQUESTER_ID))));
    }

    private void givenProjectPermission(SubjectAttributes subject, PermissionType permissionType, boolean allowed) {
        givenProjectPermission(subject, PROJECT_ID, permissionType, allowed);
    }

    private void givenProjectPermission(
        SubjectAttributes subject,
        Long projectId,
        PermissionType permissionType,
        boolean allowed
    ) {
        given(checkPermissionUseCase.check(
            eq(subject),
            eq(ResourcePermission.of(ResourceType.PROJECT, projectId, permissionType))
        )).willReturn(allowed);
    }

    private void givenApplicationPermission(SubjectAttributes subject, boolean allowed) {
        givenApplicationPermission(subject, PROJECT_ID, allowed);
    }

    private void givenApplicationPermission(SubjectAttributes subject, Long projectId, boolean allowed) {
        given(checkPermissionUseCase.check(
            eq(subject),
            eq(ResourcePermission.of(ResourceType.PROJECT_APPLICATION, projectId, PermissionType.WRITE))
        )).willReturn(allowed);
    }

    private void givenApplicationCreatePermissions(SubjectAttributes subject, Long projectId) {
        givenProjectPermission(subject, projectId, PermissionType.READ, true);
        givenProjectPermission(subject, projectId, PermissionType.EDIT, false);
        givenProjectPermission(subject, projectId, PermissionType.DELETE, false);
        givenProjectPermission(subject, projectId, PermissionType.MANAGE, false);
        givenApplicationPermission(subject, projectId, true);
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .memberId(REQUESTER_ID)
            .schoolId(3L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .build();
    }

    private Project project(ProjectStatus status, Long ownerId, Long creatorId, String name) {
        return project(PROJECT_ID, GISU_ID, CHAPTER_ID, status, ownerId, creatorId, name);
    }

    private Project project(
        Long projectId,
        Long gisuId,
        Long chapterId,
        ProjectStatus status,
        Long ownerId,
        Long creatorId,
        String name
    ) {
        Project project = Project.createDraft(gisuId, chapterId, ownerId, 3L, creatorId);
        ReflectionTestUtils.setField(project, "id", projectId);
        ReflectionTestUtils.setField(project, "status", status);
        ReflectionTestUtils.setField(project, "name", name);
        return project;
    }

    private ChallengerInfo challenger(ChallengerPart part) {
        return ChallengerInfo.builder()
            .memberId(REQUESTER_ID)
            .gisuId(GISU_ID)
            .part(part)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .build();
    }

    private ProjectMatchingRound openRound() {
        return ProjectMatchingRound.create(
            "1차 매칭",
            null,
            MatchingType.PLAN_DEVELOPER,
            MatchingPhase.FIRST,
            CHAPTER_ID,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(60),
            Instant.now().plusSeconds(120)
        );
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> anyCollection() {
        return any(Collection.class);
    }
}
