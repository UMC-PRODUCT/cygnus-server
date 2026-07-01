package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand.AnswerEntry;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.out.IssueRecruitingApplicationNoPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.survey.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.survey.application.port.in.command.dto.AnswerCommand;
import com.umc.product.survey.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.survey.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.survey.application.port.in.command.dto.UpdateDraftFormResponseCommand;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationCommandServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;

    @Mock
    IssueRecruitingApplicationNoPort issueApplicationNoPort;

    @InjectMocks
    RecruitingApplicationCommandService sut;

    @Test
    @DisplayName("지원서_draft를_생성하며_survey_draft_response를_생성한다")
    void createDraftCreatesSurveyDraftResponse() {
        // Given
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadApplicationPort.existsByRoundIdAndApplicantIdentityKey(10L, "identity:1")).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
            1L,
            10L,
            "identity:1"
        )).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(1L, "identity:1"))
            .willReturn(false);
        given(issueApplicationNoPort.issue()).willReturn("APP-001");
        given(manageFormResponseUseCase.createDraft(any())).willReturn(700L);
        given(saveApplicationPort.save(any())).willAnswer(invocation -> {
            RecruitingApplication application = invocation.getArgument(0);
            ReflectionTestUtils.setField(application, "id", 900L);
            return application;
        });

        // When
        RecruitingApplicationInfo result = sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantMemberId(200L)
            .applicantIdentityKey("identity:1")
            .maskedEmail("a***@umc.test")
            .build());

        // Then
        assertThat(result.applicationId()).isEqualTo(900L);
        assertThat(result.applicationNo()).isEqualTo("APP-001");
        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.DRAFT);
        ArgumentCaptor<CreateDraftFormResponseCommand> responseCaptor =
            ArgumentCaptor.forClass(CreateDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().createDraft(responseCaptor.capture());
        assertThat(responseCaptor.getValue().formId()).isEqualTo(500L);
        assertThat(responseCaptor.getValue().respondentMemberId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("같은_차수에_이미_지원한_지원자는_다시_draft를_만들_수_없다")
    void createDraftRejectsSameRoundDuplicate() {
        // Given
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadApplicationPort.existsByRoundIdAndApplicantIdentityKey(10L, "identity:1")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantIdentityKey("identity:1")
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_ALREADY_EXISTS);
        then(manageFormResponseUseCase).should(never()).createDraft(any());
    }

    @Test
    @DisplayName("같은_기수의_다른_학교에_지원한_지원자는_지원할_수_없다")
    void createDraftRejectsDifferentSchoolApplication() {
        // Given
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadApplicationPort.existsByRoundIdAndApplicantIdentityKey(10L, "identity:1")).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
            1L,
            10L,
            "identity:1"
        )).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantIdentityKey("identity:1")
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS);
        then(saveApplicationPort).should(never()).save(any());
    }

    @Test
    @DisplayName("이전_차수에_진행중이거나_합격한_지원서가_있으면_재지원할_수_없다")
    void createDraftRejectsBlockingPreviousApplication() {
        // Given
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(loadApplicationPort.existsByRoundIdAndApplicantIdentityKey(10L, "identity:1")).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
            1L,
            10L,
            "identity:1"
        )).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(1L, "identity:1"))
            .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantIdentityKey("identity:1")
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_REAPPLICATION_BLOCKED);
    }

    @Test
    @DisplayName("지원서_draft_수정은_survey_draft_update로_위임한다")
    void updateDraftDelegatesSurveyDraftUpdate() {
        // Given
        RecruitingApplication application = draftApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When
        RecruitingApplicationInfo result = sut.updateDraft(UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .answers(List.of(AnswerEntry.builder()
                .questionId(1L)
                .textValue("답변")
                .build()))
            .build());

        // Then
        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.DRAFT);
        ArgumentCaptor<UpdateDraftFormResponseCommand> captor =
            ArgumentCaptor.forClass(UpdateDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().updateDraft(captor.capture());
        assertThat(captor.getValue().formResponseId()).isEqualTo(700L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(200L);
        assertThat(captor.getValue().answers())
            .extracting(AnswerCommand::questionId)
            .containsExactly(1L);
    }

    @Test
    @DisplayName("지원서_draft_제출은_survey_submitDraft_후_SUBMITTED로_변경한다")
    void submitDraftDelegatesSurveySubmitAndChangesStatus() {
        // Given
        RecruitingApplication application = draftApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(loadApplicationPort.existsByRoundIdAndApplicantIdentityKeyAndIdNot(10L, "identity:1", 900L))
            .willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
            1L,
            10L,
            "identity:1",
            900L
        )).willReturn(false);
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
            1L,
            "identity:1",
            900L
        )).willReturn(false);

        // When
        RecruitingApplicationInfo result = sut.submit(SubmitRecruitingApplicationCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .submittedIp("127.0.0.1")
            .build());

        // Then
        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        ArgumentCaptor<SubmitDraftFormResponseCommand> captor =
            ArgumentCaptor.forClass(SubmitDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().submitDraft(captor.capture());
        assertThat(captor.getValue().formResponseId()).isEqualTo(700L);
        assertThat(captor.getValue().submittedIp()).isEqualTo("127.0.0.1");
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("지원서_철회는_survey_response를_삭제하지_않고_recruiting_상태만_CANCELLED로_변경한다")
    void cancelDoesNotDeleteSurveyResponse() {
        // Given
        RecruitingApplication application = draftApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When
        RecruitingApplicationInfo result = sut.cancel(CancelRecruitingApplicationCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .reason("지원 취소")
            .build());

        // Then
        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.CANCELLED);
        then(manageFormResponseUseCase).shouldHaveNoInteractions();
        then(saveApplicationPort).should().save(application);
    }

    private RecruitingApplication draftApplication() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            publishedForm(),
            700L,
            200L,
            "identity:1",
            "APP-001",
            "a***@umc.test"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        return application;
    }

    private RecruitingApplicationForm publishedForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(
            regularRound(),
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish();
        return form;
    }

    private RecruitingRound regularRound() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", 10L);
        return round;
    }
}
