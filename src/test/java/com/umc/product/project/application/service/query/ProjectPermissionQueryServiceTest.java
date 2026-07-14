package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionReason;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
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
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

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
    ProjectPolicyAuthorizationService projectPolicyAuthorizationService;
    @Mock
    ProjectPolicySubjectSnapshot projectPolicySubjectSnapshot;
    @Mock
    PolicyDecision allowedDecision;
    @Mock
    PolicyDecision deniedDecision;

    @InjectMocks
    ProjectPermissionQueryService sut;

    @BeforeEach
    void setUpPolicyDecisions() {
        lenient().when(projectPolicyAuthorizationService.snapshot(any(SubjectAttributes.class)))
            .thenReturn(projectPolicySubjectSnapshot);
        lenient().when(projectPolicyAuthorizationService.evaluate(
            eq(projectPolicySubjectSnapshot), any(ProjectPolicyAction.class), any(ProjectPolicyResourceContext.class)
        )).thenReturn(deniedDecision);
        lenient().when(projectPolicyAuthorizationService.evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.CAPABILITY_LIST),
            any(ProjectPolicyResourceContext.class)
        )).thenReturn(allowedDecision);
        lenient().when(allowedDecision.effect()).thenReturn(PolicyEffect.ALLOW);
        lenient().when(deniedDecision.effect()).thenReturn(PolicyEffect.DENY);
        lenient().when(projectPolicySubjectSnapshot.evaluatedAt())
            .thenReturn(EVALUATED_AT);
        lenient().when(allowedDecision.outcome(ProjectPolicyOutcomes.FORM_VIEW))
            .thenReturn(Optional.of(new PolicyValue.EnumValue("FULL")));
        lenient().when(allowedDecision.outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS))
            .thenReturn(Optional.of(new PolicyValue.LongSetValue(Set.of(PROJECT_ID))));
    }

    @Test
    void DRAFT_프로젝트는_작성자이고_이름과_지원폼이_있으면_검토_요청이_가능하다() {
        Project project = project(ProjectStatus.DRAFT, REQUESTER_ID, REQUESTER_ID, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenProjectPermissions(true, true, true, false);
        givenPolicyDecision(ProjectPolicyAction.FORM_READ, true);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT, true);
        givenPolicyDecision(ProjectPolicyAction.STATISTICS_PROJECT, true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canRequestReview().allowed()).isTrue();
        assertThat(result.applicationForm().canRead().allowed()).isTrue();
        assertThat(result.application().canReadList().allowed()).isTrue();
        assertThat(result.statistics().canRead().allowed()).isTrue();
        assertThat(result.applicationForm().canCreate().allowed()).isFalse();
        assertThat(result.applicationForm().canCreate().reasonCode())
            .isEqualTo(ProjectPermissionReason.NOT_IMPLEMENTED.name());
        assertThat(result.applicationForm().canCreate().reason())
            .isEqualTo("아직은 프로젝트에 여러 개의 폼을 연결하는 것을 허용하지 않아요.");
        assertThat(result.applicationForm().canPublish().reason())
            .isEqualTo("아직은 지원 폼 공개를 별도로 지원하지 않아요.");
        assertThat(result.applicationForm().canDelete().reason())
            .isEqualTo("아직은 지원 폼 삭제를 별도로 지원하지 않아요.");
        assertThat(result.status().canComplete().reasonCode())
            .isEqualTo(ProjectPermissionReason.NOT_IMPLEMENTED.name());
        assertThat(result.status().canComplete().reason())
            .isEqualTo("아직 프로젝트 완료 처리를 지원하지 않아요.");
        then(allowedDecision).should().outcome(ProjectPolicyOutcomes.FORM_VIEW);
        then(projectPolicyAuthorizationService).should().evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.FORM_READ),
            any(ProjectPolicyResourceContext.class)
        );
        then(projectPolicyAuthorizationService).should().evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT),
            any(ProjectPolicyResourceContext.class)
        );
        then(projectPolicyAuthorizationService).should().evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.STATISTICS_PROJECT),
            any(ProjectPolicyResourceContext.class)
        );
    }

    @Test
    void 권한이_없으면_세부_상태보다_PERMISSION_DENIED가_우선한다() {
        Project project = project(ProjectStatus.DRAFT, 999L, 999L, null);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms();
        givenProjectPermissions(false, false, false, false);

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
        givenProjectPermissions(true, false, false, true);
        givenPolicyDecision(ProjectPolicyAction.FORM_READ, true);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT, true);
        givenPolicyDecision(ProjectPolicyAction.STATISTICS_PROJECT, true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canPublish().allowed()).isTrue();
        assertThat(result.status().canAbort().allowed()).isFalse();
        assertThat(result.status().canAbort().reasonCode())
            .isEqualTo(ProjectPermissionReason.INVALID_PROJECT_STATUS.name());
        assertThat(result.status().canAbort().reason()).isEqualTo("현재 진행 중인 프로젝트만 중단 시킬 수 있어요.");
    }

    @Test
    void IN_PROGRESS_프로젝트는_운영진이면_중단_가능하고_활성_차수가_있으면_지원폼_수정은_불가하다() {
        Project project = project(ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
        ProjectApplicationForm form = ProjectApplicationForm.create(project, 500L);
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms(form);
        givenProjectPermissions(true, true, true, true);
        givenPolicyDecision(ProjectPolicyAction.FORM_READ, true);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT, true);
        givenPolicyDecision(ProjectPolicyAction.STATISTICS_PROJECT, true);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.status().canAbort().allowed()).isTrue();
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
        givenProjectPermissions(true, false, false, false);
        givenPolicyDecision(ProjectPolicyAction.FORM_READ, true);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_CREATE, true);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challenger(ChallengerPart.WEB)));
        given(loadProjectMemberPort.existsByGisuAndMember(GISU_ID, REQUESTER_ID)).willReturn(false);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.application().canCreate().allowed()).isTrue();
    }

    @Test
    void 지원서_결정_capability는_APPLICATION_DECIDE_정책_결과를_사용한다() {
        Project project = project(ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms();
        givenProjectPermissions(false, false, false, false);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_DECIDE, true);

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.application().canDecide().allowed()).isTrue();
        then(projectPolicyAuthorizationService).should().evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.APPLICATION_DECIDE),
            any(ProjectPolicyResourceContext.class)
        );
    }

    @Test
    void 같은_지부와_기수의_프로젝트는_권한_계산용_조회_결과를_캐시한다() {
        Project firstProject = project(
            PROJECT_ID, GISU_ID, CHAPTER_ID, ProjectStatus.IN_PROGRESS, 999L, 999L, "서비스 리뉴얼");
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
        givenProjectPermissions(true, false, false, false);
        givenPolicyDecision(ProjectPolicyAction.FORM_READ, true);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_CREATE, true);
        given(getChallengerUseCase.findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID))
            .willReturn(Optional.of(challenger(ChallengerPart.WEB)));
        given(loadProjectMemberPort.existsByGisuAndMember(GISU_ID, REQUESTER_ID)).willReturn(false);
        given(loadProjectMatchingRoundPort.listOpenAt(eq(CHAPTER_ID), any(Instant.class)))
            .willReturn(List.of(openRound()));

        List<ProjectPermissionInfo> results = sut.listByProjectIds(
            REQUESTER_ID,
            List.of(PROJECT_ID, SECOND_PROJECT_ID)
        );

        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(result -> result.application().canCreate().allowed())
            .containsExactly(true, true);
        then(loadProjectMatchingRoundPort).should(times(1)).listOpenAt(CHAPTER_ID, EVALUATED_AT);
        then(getChallengerUseCase).should(times(1)).findByMemberIdAndGisuId(REQUESTER_ID, GISU_ID);
        then(loadProjectMemberPort).should(times(1)).existsByGisuAndMember(GISU_ID, REQUESTER_ID);
        then(checkPermissionUseCase).should(times(1)).loadSubject(REQUESTER_ID);
        then(projectPolicyAuthorizationService).should(times(1)).snapshot(subject);
        then(projectPolicyAuthorizationService).should(times(1)).evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.CAPABILITY_LIST),
            any(ProjectPolicyResourceContext.class)
        );
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

    @Test
    void CAPABILITY_LIST_정책이_DENY이면_프로젝트_조회_전에_403으로_거부한다() {
        SubjectAttributes subject = subject();
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(projectPolicyAuthorizationService.evaluate(
            eq(projectPolicySubjectSnapshot),
            eq(ProjectPolicyAction.CAPABILITY_LIST),
            any(ProjectPolicyResourceContext.class)
        )).willReturn(deniedDecision);

        assertThatThrownBy(() -> sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)))
            .isInstanceOf(AuthorizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);

        then(loadProjectPort).should(never()).listByIds(any());
    }

    @Test
    void 지원서_목록_정책이_ALLOW여도_projectIds_outcome이_없으면_capability를_거부한다() {
        Project project = project(ProjectStatus.IN_PROGRESS, REQUESTER_ID, REQUESTER_ID, "서비스 리뉴얼");
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms();
        givenProjectPermissions(false, false, false, false);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT, true);
        given(allowedDecision.outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS))
            .willReturn(Optional.empty());

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.application().canReadList().allowed()).isFalse();
        assertThat(result.application().canReadList().reasonCode())
            .isEqualTo(ProjectPermissionReason.PERMISSION_DENIED.name());
    }

    @Test
    void 지원서_목록_정책의_projectIds가_현재_프로젝트를_포함하지_않으면_capability를_거부한다() {
        Project project = project(ProjectStatus.IN_PROGRESS, REQUESTER_ID, REQUESTER_ID, "서비스 리뉴얼");
        SubjectAttributes subject = subject();
        givenBase(project, subject);
        givenForms();
        givenProjectPermissions(false, false, false, false);
        givenPolicyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT, true);
        given(allowedDecision.outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS))
            .willReturn(Optional.of(new PolicyValue.LongSetValue(Set.of(999L))));

        ProjectPermissionInfo result = sut.listByProjectIds(REQUESTER_ID, List.of(PROJECT_ID)).get(0);

        assertThat(result.application().canReadList().allowed()).isFalse();
        assertThat(result.application().canReadList().reasonCode())
            .isEqualTo(ProjectPermissionReason.PERMISSION_DENIED.name());
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

    private void givenProjectPermissions(boolean read, boolean edit, boolean delete, boolean manage) {
        givenPolicyDecision(ProjectPolicyAction.PROJECT_MEMBER_LIST, read);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_UPDATE, edit);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_SUBMIT, edit);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP, edit);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_MEMBER_ADD, edit);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_MEMBER_REMOVE, edit);
        givenPolicyDecision(ProjectPolicyAction.FORM_UPDATE, edit);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_PUBLISH, manage);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_QUOTA_UPDATE, manage);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_ABORT, manage);
        givenPolicyDecision(ProjectPolicyAction.PROJECT_DELETE, delete);
    }

    private void givenPolicyDecision(ProjectPolicyAction action, boolean allowed) {
        given(projectPolicyAuthorizationService.evaluate(
            eq(projectPolicySubjectSnapshot), eq(action), any(ProjectPolicyResourceContext.class)
        )).willReturn(allowed ? allowedDecision : deniedDecision);
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
            GISU_ID,
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
