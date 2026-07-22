package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.port.in.command.dto.ApplicationDecisionStatus;
import com.umc.product.project.application.port.in.command.dto.CancelProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectApplicationDraftCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationPort;
import com.umc.product.project.application.service.policy.DesignerMatchingPolicy;
import com.umc.product.project.application.service.policy.DeveloperMatchingPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectApplicationCommandServiceTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long APPLICATION_ID = 500L;
    private static final Long APPLICATION_FORM_ID = 600L;
    private static final Long FORM_ID = 700L;
    private static final Long FORM_RESPONSE_ID = 999L;
    private static final Long MATCHING_ROUND_ID = 800L;
    private static final Long APPLICANT_MEMBER_ID = 100L;
    private static final Long DECIDER_MEMBER_ID = 200L;
    private static final Long COMMON_SECTION_ID = 10L;
    private static final Long WEB_SECTION_ID = 20L;
    private static final Long DESIGN_SECTION_ID = 30L;
    private static final Long COMMON_QUESTION_ID = 101L;
    private static final Long WEB_QUESTION_ID = 201L;
    private static final Long DESIGN_QUESTION_ID = 301L;

    // 합/불(decide)은 지원 종료(endsAt) 후 ~ 결정 마감(decisionDeadline) 전에만 가능하므로
    // 공용 라운드는 지원이 끝난 상태(endsAt 과거, decisionDeadline 미래)로 둔다.
    // 지원 진행 중이어야 하는 cancel 성공 케이스는 개별 테스트에서 endsAt 을 미래로 오버라이드한다.
    private static final Instant NOW = Instant.now();
    private static final Instant ROUND_STARTS_AT = NOW.minusSeconds(172_800);
    private static final Instant ROUND_ENDS_AT = NOW.minusSeconds(43_200);
    private static final Instant ROUND_DECISION_DEADLINE = NOW.plusSeconds(86_400);

    @Mock
    LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    SaveProjectApplicationPort saveProjectApplicationPort;
    @Mock
    LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    @Mock
    LoadProjectApplicationFormPolicyPort loadProjectApplicationFormPolicyPort;
    @Mock
    LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;
    @Mock
    GetFormUseCase getFormUseCase;

    ProjectApplicationCommandService sut;

    @BeforeEach
    void setUpQuotaMocks() {
        sut = new ProjectApplicationCommandService(
            loadProjectApplicationPort,
            saveProjectApplicationPort,
            loadProjectApplicationFormPort,
            loadProjectApplicationFormPolicyPort,
            loadProjectPartQuotaPort,
            loadProjectMemberPort,
            loadProjectMatchingRoundPort,
            manageFormResponseUseCase,
            getChallengerUseCase,
            checkChallengerAuthorityUseCase,
            List.of(new DeveloperMatchingPolicy(), new DesignerMatchingPolicy()),
            getFormUseCase
        );

        // 기본: quota 충분 (ChallengerPart.WEB, TO 6, active 0, 같은 차수 APPROVED 0)
        given(getChallengerUseCase.getByMemberIdAndGisuId(any(), any()))
            .willReturn(challengerWithPart(ChallengerPart.WEB));
        given(loadProjectPartQuotaPort.listByProjectId(any()))
            .willReturn(List.of(partQuotaForWeb(6L)));
        given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
            .willReturn(Map.of());
        given(loadProjectApplicationPort.listByMatchingRoundId(any()))
            .willReturn(List.of());
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
            .willReturn(Map.of(APPLICANT_MEMBER_ID, challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.WEB)));
    }

    @Nested
    class createDraft {

        @Test
        void 모집중인_파트와_차수에_새_지원서_초안을_생성한다() {
            ProjectApplicationForm form = applicableForm(999L);
            ProjectMatchingRound round = acceptingRound(MatchingType.PLAN_DEVELOPER);
            givenCreateBase(form, round);
            given(manageFormResponseUseCase.createDraft(any(CreateDraftFormResponseCommand.class)))
                .willReturn(FORM_RESPONSE_ID);
            given(saveProjectApplicationPort.save(any(ProjectApplication.class))).willAnswer(invocation -> {
                ProjectApplication saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", APPLICATION_ID);
                return saved;
            });

            ProjectApplicationInfo result = sut.create(createCommand());

            assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(result.status()).isEqualTo(ProjectApplicationStatus.DRAFT);
            ArgumentCaptor<CreateDraftFormResponseCommand> captor =
                ArgumentCaptor.forClass(CreateDraftFormResponseCommand.class);
            then(manageFormResponseUseCase).should().createDraft(captor.capture());
            assertThat(captor.getValue().formId()).isEqualTo(FORM_ID);
            assertThat(captor.getValue().respondentMemberId()).isEqualTo(APPLICANT_MEMBER_ID);
        }

        @Test
        void 지원폼이_없으면_생성을_거부한다() {
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.empty());

            assertCreateRejected(ProjectErrorCode.APPLICATION_FORM_NOT_FOUND);
        }

        @Test
        void 모집중이_아니거나_자기_프로젝트면_생성을_거부한다() {
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID))
                .willReturn(Optional.of(applicationForm()));
            assertCreateRejected(ProjectErrorCode.PROJECT_INVALID_STATE);

            ProjectApplicationForm selfForm = applicableForm(APPLICANT_MEMBER_ID);
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(selfForm));
            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_SELF_APPLY_NOT_ALLOWED);
        }

        @Test
        void 모집하지_않는_파트거나_이미_팀원이면_생성을_거부한다() {
            ProjectApplicationForm form = applicableForm(999L);
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
            given(loadProjectPartQuotaPort.existsByProjectIdAndPart(PROJECT_ID, ChallengerPart.WEB))
                .willReturn(false);
            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_PART_NOT_ALLOWED);

            given(loadProjectPartQuotaPort.existsByProjectIdAndPart(PROJECT_ID, ChallengerPart.WEB))
                .willReturn(true);
            given(loadProjectMemberPort.existsByGisuAndMember(1L, APPLICANT_MEMBER_ID)).willReturn(true);
            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_MEMBER_ALREADY_IN_TEAM);
        }

        @Test
        void 닫힌_차수거나_파트와_차수_type이_다르면_생성을_거부한다() {
            ProjectApplicationForm form = applicableForm(999L);
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
            given(loadProjectPartQuotaPort.existsByProjectIdAndPart(PROJECT_ID, ChallengerPart.WEB))
                .willReturn(true);
            given(loadProjectMemberPort.existsByGisuAndMember(1L, APPLICANT_MEMBER_ID)).willReturn(false);
            given(loadProjectMatchingRoundPort.getById(MATCHING_ROUND_ID)).willReturn(openRound());
            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_ROUND_NOT_OPEN);

            given(loadProjectMatchingRoundPort.getById(MATCHING_ROUND_ID))
                .willReturn(acceptingRound(MatchingType.PLAN_DESIGN));
            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_ROUND_TYPE_MISMATCH);
        }

        @Test
        void 같은_차수의_DRAFT가_이미_있으면_중복_생성을_거부한다() {
            ProjectApplicationForm form = applicableForm(999L);
            ProjectMatchingRound round = acceptingRound(MatchingType.PLAN_DEVELOPER);
            givenCreateBase(form, round);
            given(loadProjectApplicationPort.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
                PROJECT_ID, APPLICANT_MEMBER_ID, MATCHING_ROUND_ID, ProjectApplicationStatus.DRAFT))
                .willReturn(Optional.of(ProjectApplication.create(
                    form, FORM_RESPONSE_ID, APPLICANT_MEMBER_ID, round)));

            assertCreateRejected(ProjectErrorCode.PROJECT_APPLICATION_ALREADY_EXISTS);
            then(manageFormResponseUseCase).should(never()).createDraft(any());
        }

        @Test
        void 디자인_파트는_PLAN_DESIGN_차수에_초안을_생성한다() {
            ProjectApplicationForm form = applicableForm(999L);
            ProjectMatchingRound round = acceptingRound(MatchingType.PLAN_DESIGN);
            given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
            given(getChallengerUseCase.getByMemberIdAndGisuId(APPLICANT_MEMBER_ID, 1L))
                .willReturn(challengerWithPart(ChallengerPart.DESIGN));
            given(loadProjectPartQuotaPort.existsByProjectIdAndPart(PROJECT_ID, ChallengerPart.DESIGN))
                .willReturn(true);
            given(loadProjectMemberPort.existsByGisuAndMember(1L, APPLICANT_MEMBER_ID)).willReturn(false);
            given(loadProjectMatchingRoundPort.getById(MATCHING_ROUND_ID)).willReturn(round);
            given(loadProjectApplicationPort.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
                PROJECT_ID, APPLICANT_MEMBER_ID, MATCHING_ROUND_ID, ProjectApplicationStatus.DRAFT))
                .willReturn(Optional.empty());
            given(manageFormResponseUseCase.createDraft(any())).willReturn(FORM_RESPONSE_ID);
            given(saveProjectApplicationPort.save(any())).willAnswer(invocation -> {
                ProjectApplication saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", APPLICATION_ID);
                return saved;
            });

            assertThat(sut.create(createCommand()).applicationId()).isEqualTo(APPLICATION_ID);
        }
    }

    @Test
    void submit은_닫힌_차수와_동일차수_중복제출을_거부한다() {
        ProjectApplication closed = applicationWithStatus(ProjectApplicationStatus.DRAFT, openRound());
        given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID)).willReturn(Optional.of(closed));
        assertThatThrownBy(() -> sut.submit(submitCommand()))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_ROUND_NOT_OPEN);

        ProjectApplication duplicated = applicationWithStatus(
            ProjectApplicationStatus.DRAFT, acceptingRound(MatchingType.PLAN_DESIGN));
        given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID)).willReturn(Optional.of(duplicated));
        given(loadProjectApplicationPort.existsByRoundAndApplicantAndStatus(
            MATCHING_ROUND_ID, APPLICANT_MEMBER_ID, ProjectApplicationStatus.SUBMITTED)).willReturn(true);
        assertThatThrownBy(() -> sut.submit(submitCommand()))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_DUPLICATE_SUBMISSION);
    }

    @Test
    void SUPER_ADMIN은_빈_지원자_목록에서도_강제_거절할_수_있다() {
        ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
        given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(
            MATCHING_ROUND_ID, PROJECT_ID)).willReturn(List.of());
        given(checkChallengerAuthorityUseCase.isSuperAdmin(DECIDER_MEMBER_ID)).willReturn(true);
        given(saveProjectApplicationPort.save(application)).willReturn(application);

        assertThat(sut.decide(
            APPLICATION_ID, ApplicationDecisionStatus.REJECTED, "강제 거절", DECIDER_MEMBER_ID).status())
            .isEqualTo(ProjectApplicationStatus.REJECTED);
    }

    @Test
    void 지원_정책이_없으면_거절_결정을_fail_closed한다() {
        ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
        given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(
            MATCHING_ROUND_ID, PROJECT_ID)).willReturn(List.of());
        ReflectionTestUtils.setField(sut, "matchingDecisionPolicies", List.of());

        assertThatThrownBy(() -> sut.decide(
            APPLICATION_ID, ApplicationDecisionStatus.REJECTED, null, DECIDER_MEMBER_ID))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_MATCHING_ROUND_POLICY_NOT_FOUND);
    }

    @Test
    void update는_null_ID_미존재_ID_null_답변과_중복_정책을_안전하게_처리한다() {
        assertThatThrownBy(() -> sut.update(UpdateProjectApplicationDraftCommand.builder()
            .projectId(PROJECT_ID).applicationId(null).requesterMemberId(APPLICANT_MEMBER_ID)
            .answers(List.of()).build()))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);

        given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> sut.update(UpdateProjectApplicationDraftCommand.builder()
            .projectId(PROJECT_ID).applicationId(APPLICATION_ID).requesterMemberId(APPLICANT_MEMBER_ID)
            .answers(List.of()).build()))
            .isInstanceOf(ProjectDomainException.class);

        ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
        given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID)).willReturn(Optional.of(application));
        givenVisibleScope(application);
        assertThatThrownBy(() -> sut.update(UpdateProjectApplicationDraftCommand.builder()
            .projectId(PROJECT_ID).applicationId(APPLICATION_ID).requesterMemberId(APPLICANT_MEMBER_ID)
            .answers(null).build()))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);

        ProjectApplicationForm form = application.getApplicationForm();
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID))
            .willReturn(List.of(
                ProjectApplicationFormPolicy.createForParts(form, WEB_SECTION_ID, Set.of(ChallengerPart.DESIGN)),
                ProjectApplicationFormPolicy.createForParts(form, WEB_SECTION_ID, Set.of(ChallengerPart.WEB)),
                ProjectApplicationFormPolicy.createCommon(form, COMMON_SECTION_ID)));
        assertThat(sut.update(UpdateProjectApplicationDraftCommand.builder()
            .projectId(PROJECT_ID).applicationId(APPLICATION_ID).requesterMemberId(APPLICANT_MEMBER_ID)
            .answers(List.of(answerEntry(WEB_QUESTION_ID))).build()).applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Nested
    class updateAndSubmit {

        @Test
        @DisplayName("update는 applicationId로 지정한 DRAFT만 수정한다")
        void update는_applicationId로_지정한_DRAFT만_수정한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID))
                .willReturn(Optional.of(application));
            givenVisibleScope(application);

            ProjectApplicationInfo result = sut.update(UpdateProjectApplicationDraftCommand.builder()
                .projectId(PROJECT_ID)
                .applicationId(APPLICATION_ID)
                .requesterMemberId(APPLICANT_MEMBER_ID)
                .answers(List.of(answerEntry(COMMON_QUESTION_ID)))
                .build());

            ArgumentCaptor<UpdateDraftFormResponseCommand> captor =
                ArgumentCaptor.forClass(UpdateDraftFormResponseCommand.class);
            then(manageFormResponseUseCase).should().updateDraft(captor.capture());
            assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(captor.getValue().formResponseId()).isEqualTo(FORM_RESPONSE_ID);
            assertThat(captor.getValue().answers())
                .extracting(AnswerCommand::questionId)
                .containsExactly(COMMON_QUESTION_ID);
            then(loadProjectApplicationPort).should(never()).getDraftByProjectAndMember(anyLong(), anyLong());
        }

        @Test
        @DisplayName("update 요청에 노출되지 않은 questionId가 있으면 실패한다")
        void update_요청에_노출되지_않은_questionId가_있으면_실패한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID))
                .willReturn(Optional.of(application));
            givenVisibleScope(application);

            assertThatThrownBy(() -> sut.update(UpdateProjectApplicationDraftCommand.builder()
                .projectId(PROJECT_ID)
                .applicationId(APPLICATION_ID)
                .requesterMemberId(APPLICANT_MEMBER_ID)
                .answers(List.of(answerEntry(DESIGN_QUESTION_ID)))
                .build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);

            then(manageFormResponseUseCase).should(never()).updateDraft(any());
        }

        @Test
        @DisplayName("submit은 지원자에게 노출되는 required question scope만 Survey에 전달한다")
        void submit은_노출되는_required_question_scope만_Survey에_전달한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            ReflectionTestUtils.setField(application.getAppliedMatchingRound(), "endsAt", NOW.plusSeconds(43_200));
            given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID))
                .willReturn(Optional.of(application));
            givenVisibleScope(application);

            ProjectApplicationInfo result = sut.submit(SubmitProjectApplicationCommand.builder()
                .projectId(PROJECT_ID)
                .applicationId(APPLICATION_ID)
                .requesterMemberId(APPLICANT_MEMBER_ID)
                .build());

            ArgumentCaptor<SubmitDraftFormResponseCommand> captor =
                ArgumentCaptor.forClass(SubmitDraftFormResponseCommand.class);
            then(manageFormResponseUseCase).should().submitDraft(captor.capture());
            assertThat(result.status()).isEqualTo(ProjectApplicationStatus.SUBMITTED);
            assertThat(captor.getValue().formResponseId()).isEqualTo(FORM_RESPONSE_ID);
            assertThat(captor.getValue().requiredQuestionIds())
                .containsExactlyInAnyOrder(COMMON_QUESTION_ID, WEB_QUESTION_ID);
            assertThat(captor.getValue().allowedQuestionIds())
                .containsExactlyInAnyOrder(COMMON_QUESTION_ID, WEB_QUESTION_ID);
            then(loadProjectApplicationPort).should(never()).getDraftByProjectAndMember(anyLong(), anyLong());
        }

        @Test
        @DisplayName("path projectId와 application의 projectId가 다르면 PROJECT_APPLICATION_NOT_FOUND")
        void path_projectId와_application_projectId가_다르면_NOT_FOUND() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID))
                .willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.update(UpdateProjectApplicationDraftCommand.builder()
                .projectId(PROJECT_ID + 1)
                .applicationId(APPLICATION_ID)
                .requesterMemberId(APPLICANT_MEMBER_ID)
                .answers(List.of(answerEntry(COMMON_QUESTION_ID)))
                .build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);

            then(manageFormResponseUseCase).should(never()).updateDraft(any());
        }

        @Test
        @DisplayName("DRAFT가 아닌 applicationId로 submit하면 실패한다")
        void DRAFT가_아닌_applicationId로_submit하면_실패한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findByIdWithDetails(APPLICATION_ID))
                .willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.submit(SubmitProjectApplicationCommand.builder()
                .projectId(PROJECT_ID)
                .applicationId(APPLICATION_ID)
                .requesterMemberId(APPLICANT_MEMBER_ID)
                .build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_DRAFT_APPLICATION_NOT_FOUND);

            then(manageFormResponseUseCase).should(never()).submitDraft(any());
        }
    }

    @Nested
    class decide {

        @Test
        void APPROVED_입력시_도메인_approve_호출_후_저장한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            ProjectApplicationInfo info = sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, "역량 우수", DECIDER_MEMBER_ID
            );

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("역량 우수");
            assertThat(info.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(info.status()).isEqualTo(ProjectApplicationStatus.APPROVED);
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void REJECTED_입력시_도메인_reject_호출_후_저장한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(List.of(application));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.REJECTED, "면접 결과", DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
            assertThat(application.getStatusChangeReason()).isEqualTo("면접 결과");
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void 디자이너_지원자_2명_승인_0명에서_SUBMITTED를_REJECTED로_변경하면_MINIMUM_SELECTION_REQUIRED() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ProjectApplication otherSubmitted = otherApplication(
                701L, 101L, application.getApplicationForm(), ProjectApplicationStatus.SUBMITTED
            );
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(List.of(application, otherSubmitted));
            given(loadProjectPartQuotaPort.listByProjectId(any()))
                .willReturn(List.of(partQuotaFor(ChallengerPart.DESIGN, 1L)));
            given(getChallengerUseCase.getByMemberIdAndGisuId(eq(APPLICANT_MEMBER_ID), any()))
                .willReturn(challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.DESIGN));
            given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
                .willReturn(Map.of(
                    APPLICANT_MEMBER_ID, challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.DESIGN),
                    101L, challengerWith(101L, ChallengerPart.DESIGN)
                ));

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.REJECTED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_MINIMUM_SELECTION_REQUIRED);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.SUBMITTED);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void 디자이너_최소_1명이_이미_APPROVED면_다른_SUBMITTED를_REJECTED로_변경할_수_있다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ProjectApplication approved = otherApplication(
                701L, 101L, application.getApplicationForm(), ProjectApplicationStatus.APPROVED
            );
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(List.of(application, approved));
            given(loadProjectPartQuotaPort.listByProjectId(any()))
                .willReturn(List.of(partQuotaFor(ChallengerPart.DESIGN, 1L)));
            given(getChallengerUseCase.getByMemberIdAndGisuId(eq(APPLICANT_MEMBER_ID), any()))
                .willReturn(challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.DESIGN));
            given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
                .willReturn(Map.of(
                    APPLICANT_MEMBER_ID, challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.DESIGN),
                    101L, challengerWith(101L, ChallengerPart.DESIGN)
                ));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.REJECTED, null, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void 개발자_TO6_지원자6명_승인3명에서_APPROVED를_REJECTED로_변경하면_MINIMUM_SELECTION_REQUIRED() {
            ProjectMatchingRound developerRound = openRound(MatchingType.PLAN_DEVELOPER);
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.APPROVED, developerRound);
            List<ProjectApplication> applicants = List.of(
                application,
                otherApplication(701L, 101L, application.getApplicationForm(), ProjectApplicationStatus.APPROVED),
                otherApplication(702L, 102L, application.getApplicationForm(), ProjectApplicationStatus.APPROVED),
                otherApplication(703L, 103L, application.getApplicationForm(), ProjectApplicationStatus.SUBMITTED),
                otherApplication(704L, 104L, application.getApplicationForm(), ProjectApplicationStatus.SUBMITTED),
                otherApplication(705L, 105L, application.getApplicationForm(), ProjectApplicationStatus.SUBMITTED)
            );
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(applicants);
            given(loadProjectPartQuotaPort.listByProjectId(any()))
                .willReturn(List.of(partQuotaFor(ChallengerPart.WEB, 6L)));
            given(getChallengerUseCase.getByMemberIdAndGisuId(eq(APPLICANT_MEMBER_ID), any()))
                .willReturn(challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.WEB));
            given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
                .willReturn(Map.of(
                    APPLICANT_MEMBER_ID, challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.WEB),
                    101L, challengerWith(101L, ChallengerPart.WEB),
                    102L, challengerWith(102L, ChallengerPart.WEB),
                    103L, challengerWith(103L, ChallengerPart.WEB),
                    104L, challengerWith(104L, ChallengerPart.WEB),
                    105L, challengerWith(105L, ChallengerPart.WEB)
                ));

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.REJECTED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_MINIMUM_SELECTION_REQUIRED);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void APPROVED_변경은_최소선발_미달_상태에서도_quota_검증만_통과하면_허용한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ProjectApplication otherSubmitted = otherApplication(
                701L, 101L, application.getApplicationForm(), ProjectApplicationStatus.SUBMITTED
            );
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectApplicationPort.listByMatchingRoundId(any()))
                .willReturn(List.of());
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(List.of(application, otherSubmitted));
            given(loadProjectPartQuotaPort.listByProjectId(any()))
                .willReturn(List.of(partQuotaFor(ChallengerPart.DESIGN, 1L)));
            given(getChallengerUseCase.getByMemberIdAndGisuId(eq(APPLICANT_MEMBER_ID), any()))
                .willReturn(challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.DESIGN));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void reason이_null이어도_정상_처리된다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID);

            assertThat(application.getStatusChangeReason()).isNull();
        }

        @Test
        void 존재하지_않는_지원서면_PROJECT_APPLICATION_NOT_FOUND() {
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void DRAFT_상태_지원서면_도메인_예외_전파_후_저장하지_않는다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void 차수_종료_후에는_도메인_예외_전파_후_저장하지_않는다() {
            ProjectMatchingRound expiredRound = openRound();
            ReflectionTestUtils.setField(expiredRound, "decisionDeadline", NOW.minusSeconds(60));
            ProjectApplication application = ProjectApplication.create(
                applicationForm(), 999L, APPLICANT_MEMBER_ID, expiredRound
            );
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
            ReflectionTestUtils.setField(application, "status", ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_LOCKED);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        @DisplayName("SUPER_ADMIN은 결정 가능 시간과 관계없이 지원서 상태를 변경할 수 있다")
        void SUPER_ADMIN은_결정_가능_시간과_관계없이_지원서_상태를_변경할_수_있다() {
            ProjectMatchingRound expiredRound = openRound();
            ReflectionTestUtils.setField(expiredRound, "decisionDeadline", NOW.minusSeconds(60));
            ProjectApplication application = ProjectApplication.create(
                applicationForm(), 999L, APPLICANT_MEMBER_ID, expiredRound
            );
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
            ReflectionTestUtils.setField(application, "status", ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(checkChallengerAuthorityUseCase.isSuperAdmin(DECIDER_MEMBER_ID)).willReturn(true);

            ProjectApplicationInfo result = sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, "슈퍼어드민 수정", DECIDER_MEMBER_ID
            );

            assertThat(result.status()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("슈퍼어드민 수정");
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void APPROVED_토글시_active_멤버가_TO를_채워_남은_자리가_없으면_QUOTA_EXCEEDED() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
                .willReturn(Map.of(ChallengerPart.WEB, 6L));   // active 6 == TO 6 → 남은 자리 0

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_QUOTA_EXCEEDED);
            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void APPROVED_토글시_active_멤버와_현재차수_APPROVED_합산이_TO를_초과하면_QUOTA_EXCEEDED() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
                .willReturn(Map.of(ChallengerPart.WEB, 4L));   // active 4
            // 현재 차수 APPROVED 2명 (다른 application) → 4 + 2 + 1 = 7 > TO 6
            given(loadProjectApplicationPort.listByMatchingRoundId(any()))
                .willReturn(List.of(
                    otherApprovedApplication(701L, 71L, application.getApplicationForm()),
                    otherApprovedApplication(702L, 72L, application.getApplicationForm())
                ));
            given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
                .willReturn(Map.of(
                    71L, challengerWith(71L, ChallengerPart.WEB),
                    72L, challengerWith(72L, ChallengerPart.WEB)
                ));

            assertThatThrownBy(() -> sut.decide(
                APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_QUOTA_EXCEEDED);
        }

        @Test
        void 이미_APPROVED인_지원서를_다시_APPROVED로_토글하면_quota_검증_우회() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.APPROVED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            // active 6 으로 quota 초과 상황이라도 — 이미 APPROVED 인 application 재토글은 검증 우회
            given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
                .willReturn(Map.of(ChallengerPart.WEB, 6L));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.APPROVED, "재확인", DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void REJECTED_토글은_quota_검증을_적용하지_않는다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.APPROVED);
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
                .willReturn(Map.of(ChallengerPart.WEB, 99L));   // 초과 상황이지만 REJECTED 는 무관
            given(loadProjectApplicationPort.listDecidableByMatchingRoundIdAndProjectId(any(), any()))
                .willReturn(List.of(application));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.REJECTED, null, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
        }

        @Test
        void 다른_part로_APPROVED인_application은_현재_part_quota_검증에_영향을_주지_않는다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ProjectApplication iosApp = otherApprovedApplication(701L, 71L, application.getApplicationForm());
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(loadProjectMemberPort.countByProjectIdGroupByPart(any()))
                .willReturn(Map.of(ChallengerPart.WEB, 5L));   // WEB active 5, TO 6 → 남은 1
            given(loadProjectApplicationPort.listByMatchingRoundId(any()))
                .willReturn(List.of(iosApp));
            given(getChallengerUseCase.getByMemberIdAndGisuId(eq(APPLICANT_MEMBER_ID), any()))
                .willReturn(challengerWith(APPLICANT_MEMBER_ID, ChallengerPart.WEB));
            given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), any()))
                .willReturn(Map.of(71L, challengerWith(71L, ChallengerPart.IOS)));

            sut.decide(APPLICATION_ID, ApplicationDecisionStatus.APPROVED, null, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
        }
    }

    @Nested
    class cancel {

        @Test
        void DRAFT를_CANCELLED로_전이하고_저장한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.DRAFT);
            ReflectionTestUtils.setField(application.getAppliedMatchingRound(), "endsAt", NOW.plusSeconds(43_200));
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(saveProjectApplicationPort.save(any(ProjectApplication.class)))
                .willAnswer(inv -> inv.getArgument(0));

            ProjectApplicationInfo result = sut.cancel(cancelCommand("마음 바뀜"));

            assertThat(result.status()).isEqualTo(ProjectApplicationStatus.CANCELLED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(APPLICANT_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("마음 바뀜");
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void SUBMITTED를_CANCELLED로_전이하고_저장한다() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ReflectionTestUtils.setField(application.getAppliedMatchingRound(), "endsAt", NOW.plusSeconds(43_200));
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(saveProjectApplicationPort.save(any(ProjectApplication.class)))
                .willAnswer(inv -> inv.getArgument(0));

            ProjectApplicationInfo result = sut.cancel(cancelCommand(null));

            assertThat(result.status()).isEqualTo(ProjectApplicationStatus.CANCELLED);
            then(saveProjectApplicationPort).should(times(1)).save(application);
        }

        @Test
        void 차수가_종료되었으면_CANCEL_ROUND_CLOSED() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.SUBMITTED);
            ReflectionTestUtils.setField(application.getAppliedMatchingRound(), "endsAt", NOW.minusSeconds(60));
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.cancel(cancelCommand("늦었지만")))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_ROUND_CLOSED);

            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void 지원서가_없으면_PROJECT_APPLICATION_NOT_FOUND() {
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.cancel(cancelCommand(null)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);

            then(saveProjectApplicationPort).should(never()).save(any());
        }

        @Test
        void 이미_CANCELLED라면_도메인이_CANCEL_NOT_ALLOWED() {
            ProjectApplication application = applicationWithStatus(ProjectApplicationStatus.CANCELLED);
            ReflectionTestUtils.setField(application.getAppliedMatchingRound(), "endsAt", NOW.plusSeconds(43_200));
            given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            assertThatThrownBy(() -> sut.cancel(cancelCommand(null)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_NOT_ALLOWED);

            then(saveProjectApplicationPort).should(never()).save(any());
        }
    }

    private ProjectApplication applicationWithStatus(ProjectApplicationStatus status) {
        return applicationWithStatus(status, openRound());
    }

    private CreateDraftProjectApplicationCommand createCommand() {
        return CreateDraftProjectApplicationCommand.builder()
            .projectId(PROJECT_ID)
            .applicantMemberId(APPLICANT_MEMBER_ID)
            .matchingRoundId(MATCHING_ROUND_ID)
            .build();
    }

    private SubmitProjectApplicationCommand submitCommand() {
        return SubmitProjectApplicationCommand.builder()
            .projectId(PROJECT_ID)
            .applicationId(APPLICATION_ID)
            .requesterMemberId(APPLICANT_MEMBER_ID)
            .build();
    }

    private void assertCreateRejected(ProjectErrorCode errorCode) {
        assertThatThrownBy(() -> sut.create(createCommand()))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", errorCode);
    }

    private void givenCreateBase(ProjectApplicationForm form, ProjectMatchingRound round) {
        given(loadProjectApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
        given(loadProjectPartQuotaPort.existsByProjectIdAndPart(PROJECT_ID, ChallengerPart.WEB))
            .willReturn(true);
        given(loadProjectMemberPort.existsByGisuAndMember(1L, APPLICANT_MEMBER_ID)).willReturn(false);
        given(loadProjectMatchingRoundPort.getById(MATCHING_ROUND_ID)).willReturn(round);
        given(loadProjectApplicationPort.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
            PROJECT_ID, APPLICANT_MEMBER_ID, MATCHING_ROUND_ID, ProjectApplicationStatus.DRAFT))
            .willReturn(Optional.empty());
    }

    private ProjectApplicationForm applicableForm(Long ownerMemberId) {
        ProjectApplicationForm form = applicationForm();
        ReflectionTestUtils.setField(form.getProject(), "status", com.umc.product.project.domain.enums.ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(form.getProject(), "productOwnerMemberId", ownerMemberId);
        return form;
    }

    private ProjectMatchingRound acceptingRound(MatchingType matchingType) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "지원 진행 중 차수", null, matchingType, MatchingPhase.FIRST, 1L,
            NOW.minusSeconds(3_600), NOW.plusSeconds(3_600), NOW.plusSeconds(7_200)
        );
        ReflectionTestUtils.setField(round, "id", MATCHING_ROUND_ID);
        return round;
    }

    private ProjectApplication applicationWithStatus(ProjectApplicationStatus status, ProjectMatchingRound round) {
        ProjectApplication application = ProjectApplication.create(
            applicationForm(), FORM_RESPONSE_ID, APPLICANT_MEMBER_ID, round
        );
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        ReflectionTestUtils.setField(application, "status", status);
        return application;
    }

    private ProjectMatchingRound openRound() {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "기획-디자인 1차 매칭", null,
            MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 1L,
            ROUND_STARTS_AT, ROUND_ENDS_AT, ROUND_DECISION_DEADLINE
        );
        ReflectionTestUtils.setField(round, "id", MATCHING_ROUND_ID);
        return round;
    }

    private ProjectMatchingRound openRound(MatchingType matchingType) {
        return ProjectMatchingRound.create(
            "프로젝트 매칭", null,
            matchingType, MatchingPhase.FIRST, 1L,
            ROUND_STARTS_AT, ROUND_ENDS_AT, ROUND_DECISION_DEADLINE
        );
    }

    private ProjectApplicationForm applicationForm() {
        Project project = Project.createDraft(1L, 2L, 999L, 7L, 999L);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        ProjectApplicationForm applicationForm = ProjectApplicationForm.create(project, FORM_ID);
        ReflectionTestUtils.setField(applicationForm, "id", APPLICATION_FORM_ID);
        return applicationForm;
    }

    private ChallengerInfo challengerWithPart(ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(1L).memberId(APPLICANT_MEMBER_ID).gisuId(1L)
            .part(part).challengerPoints(new ArrayList<>()).totalPoints(0.0)
            .build();
    }

    private ProjectPartQuota partQuotaForWeb(Long quota) {
        return partQuotaFor(ChallengerPart.WEB, quota);
    }

    private ProjectPartQuota partQuotaFor(ChallengerPart part, Long quota) {
        Project project = Project.createDraft(1L, 2L, 999L, 7L, 999L);
        return ProjectPartQuota.create(project, part, quota, 999L);
    }

    private ProjectApplication otherApprovedApplication(Long id, Long applicantMemberId, ProjectApplicationForm form) {
        return otherApplication(id, applicantMemberId, form, ProjectApplicationStatus.APPROVED);
    }

    private ProjectApplication otherApplication(
        Long id,
        Long applicantMemberId,
        ProjectApplicationForm form,
        ProjectApplicationStatus status
    ) {
        ProjectApplication app = ProjectApplication.create(form, 999L, applicantMemberId, openRound());
        ReflectionTestUtils.setField(app, "id", id);
        ReflectionTestUtils.setField(app, "status", status);
        return app;
    }

    private ChallengerInfo challengerWith(Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(memberId * 10).memberId(memberId).gisuId(1L)
            .part(part).challengerPoints(new ArrayList<>()).totalPoints(0.0)
            .build();
    }

    private CancelProjectApplicationCommand cancelCommand(String reason) {
        return CancelProjectApplicationCommand.builder()
            .applicationId(APPLICATION_ID)
            .requesterMemberId(APPLICANT_MEMBER_ID)
            .reason(reason)
            .build();
    }

    private void givenVisibleScope(ProjectApplication application) {
        ProjectApplicationForm form = application.getApplicationForm();
        given(getChallengerUseCase.getByMemberIdAndGisuId(APPLICANT_MEMBER_ID, form.getProject().getGisuId()))
            .willReturn(challengerWithPart(ChallengerPart.WEB));
        given(loadProjectApplicationFormPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID))
            .willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, COMMON_SECTION_ID),
                ProjectApplicationFormPolicy.createForParts(form, WEB_SECTION_ID, Set.of(ChallengerPart.WEB)),
                ProjectApplicationFormPolicy.createForParts(form, DESIGN_SECTION_ID, Set.of(ChallengerPart.DESIGN))
            ));
        given(getFormUseCase.getFormWithStructure(FORM_ID))
            .willReturn(formStructure());
    }

    private FormWithStructureInfo formStructure() {
        return FormWithStructureInfo.builder()
            .formId(FORM_ID)
            .createdMemberId(1L)
            .title("프로젝트 지원서")
            .description(null)
            .status(FormStatus.PUBLISHED)
            .isAnonymous(false)
            .allowDuplicateResponses(true)
            .sections(List.of(
                section(COMMON_SECTION_ID, COMMON_QUESTION_ID, true),
                section(WEB_SECTION_ID, WEB_QUESTION_ID, true),
                section(DESIGN_SECTION_ID, DESIGN_QUESTION_ID, true)
            ))
            .build();
    }

    private FormWithStructureInfo.SectionWithQuestions section(
        Long sectionId, Long questionId, boolean isRequired
    ) {
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(sectionId)
            .title("섹션")
            .description(null)
            .orderNo(sectionId)
            .questions(List.of(question(questionId, isRequired)))
            .build();
    }

    private FormWithStructureInfo.QuestionWithOptions question(Long questionId, boolean isRequired) {
        return FormWithStructureInfo.QuestionWithOptions.builder()
            .questionId(questionId)
            .title("질문")
            .description(null)
            .type(QuestionType.SHORT_TEXT)
            .isRequired(isRequired)
            .orderNo(questionId)
            .options(List.of())
            .build();
    }

    private UpdateProjectApplicationDraftCommand.AnswerEntry answerEntry(Long questionId) {
        return UpdateProjectApplicationDraftCommand.AnswerEntry.builder()
            .questionId(questionId)
            .textValue("답변")
            .selectedOptionIds(List.of())
            .fileIds(List.of())
            .build();
    }
}
