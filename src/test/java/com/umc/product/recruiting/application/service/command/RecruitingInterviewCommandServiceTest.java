package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
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
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewCommandServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    LoadRecruitingInterviewAssignmentPort loadAssignmentPort;

    @Mock
    SaveRecruitingInterviewAssignmentPort saveAssignmentPort;

    @Mock
    LoadRecruitingInterviewEvaluationPort loadEvaluationPort;

    @Mock
    SaveRecruitingInterviewEvaluationPort saveEvaluationPort;

    @Mock
    FindRecruitingScheduleOverlapPort findScheduleOverlapPort;

    @Mock
    SendEmailPort sendEmailPort;

    @InjectMocks
    RecruitingInterviewCommandService sut;

    @Test
    @DisplayName("서류_합격_지원서에_면접을_배정하고_INTERVIEW_ASSIGNED로_변경한다")
    void assignInterviewChangesApplicationStatus() {
        // Given
        RecruitingApplication application = documentPassedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(saveAssignmentPort.saveAssignment(any())).willAnswer(invocation -> {
            RecruitingInterviewAssignment assignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", 300L);
            return assignment;
        });

        // When
        Long assignmentId = sut.assign(AssignRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .interviewerMemberId(901L)
            .startsAt(Instant.parse("2026-07-02T01:00:00Z"))
            .endsAt(Instant.parse("2026-07-02T01:30:00Z"))
            .location("온라인")
            .assignedByMemberId(1L)
            .build());

        // Then
        assertThat(assignmentId).isEqualTo(300L);
        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.INTERVIEW_ASSIGNED);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("서류_합격이_아닌_지원서에는_면접을_배정할_수_없다")
    void assignInterviewRejectsInvalidStatus() {
        // Given
        RecruitingApplication application = submittedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When & Then
        assertThatThrownBy(() -> sut.assign(AssignRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .interviewerMemberId(901L)
            .startsAt(Instant.parse("2026-07-02T01:00:00Z"))
            .endsAt(Instant.parse("2026-07-02T01:30:00Z"))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("면접을_진행하지_않는_학교는_면접_단계를_생략할_수_있다")
    void skipInterviewChangesApplicationStatus() {
        // Given
        RecruitingApplication application = documentPassedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When
        sut.skip(SkipRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .skippedByMemberId(1L)
            .reason("면접 미진행")
            .build());

        // Then
        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.INTERVIEW_SKIPPED);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("일정_후보_조회는_survey_overlap_port에_form_response_ids를_그대로_전달한다")
    void findScheduleCandidatesDelegatesSurveyOverlapPort() {
        // Given
        List<RecruitingInterviewScheduleCandidate> candidates = List.of(
            new RecruitingInterviewScheduleCandidate(
                Instant.parse("2026-07-02T01:00:00Z"),
                Instant.parse("2026-07-02T01:30:00Z"),
                2
            )
        );
        given(findScheduleOverlapPort.findOverlaps(500L, List.of(700L, 701L))).willReturn(candidates);

        // When
        List<RecruitingInterviewScheduleCandidate> result =
            sut.findScheduleCandidates(FindRecruitingInterviewScheduleCandidatesCommand.builder()
                .formId(500L)
                .formResponseIds(List.of(700L, 701L))
                .build());

        // Then
        assertThat(result).isEqualTo(candidates);
        then(findScheduleOverlapPort).should().findOverlaps(500L, List.of(700L, 701L));
    }

    @Test
    @DisplayName("면접_안내_이메일은_notification_SendEmailPort로_발송한다")
    void sendInterviewGuideUsesEmailPort() {
        // Given
        RecruitingApplication application = documentPassedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When
        sut.sendGuide(SendRecruitingInterviewGuideCommand.builder()
            .applicationId(900L)
            .recipientEmail("applicant@umc.test")
            .startsAt(Instant.parse("2026-07-02T01:00:00Z"))
            .location("온라인")
            .build());

        // Then
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(sendEmailPort).should().send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("applicant@umc.test");
        assertThat(captor.getValue().subject()).contains("면접");
        assertThat(captor.getValue().htmlBody()).contains("APP-001");
    }

    @Test
    @DisplayName("면접_평가_draft를_저장하고_제출시_중복_제출을_검증한다")
    void saveAndSubmitEvaluation() {
        // Given
        RecruitingInterviewAssignment assignment = assignment();
        RecruitingInterviewEvaluation submittedOther = RecruitingInterviewEvaluation.createDraft(assignment, 902L);
        submittedOther.submit(4, "좋음");
        given(loadAssignmentPort.getAssignmentById(300L)).willReturn(assignment);
        given(loadEvaluationPort.listSubmittedByApplicationId(900L)).willReturn(List.of(submittedOther));
        given(saveEvaluationPort.saveEvaluation(any())).willAnswer(invocation -> invocation.getArgument(0));

        // When
        sut.saveEvaluation(SaveRecruitingInterviewEvaluationCommand.builder()
            .assignmentId(300L)
            .evaluatorMemberId(901L)
            .score(3)
            .comment("초안")
            .build());
        sut.submitEvaluation(SubmitRecruitingInterviewEvaluationCommand.builder()
            .assignmentId(300L)
            .evaluatorMemberId(901L)
            .score(5)
            .comment("제출")
            .build());

        // Then
        ArgumentCaptor<RecruitingInterviewEvaluation> captor =
            ArgumentCaptor.forClass(RecruitingInterviewEvaluation.class);
        then(saveEvaluationPort).should(org.mockito.Mockito.times(2)).saveEvaluation(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus().name()).isEqualTo("SUBMITTED");
    }

    private RecruitingInterviewAssignment assignment() {
        RecruitingInterviewAssignment assignment = RecruitingInterviewAssignment.assign(
            documentPassedApplication(),
            901L,
            Instant.parse("2026-07-02T01:00:00Z"),
            Instant.parse("2026-07-02T01:30:00Z"),
            "온라인"
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);
        return assignment;
    }

    private RecruitingApplication documentPassedApplication() {
        RecruitingApplication application = submittedApplication();
        application.passDocument(1L, "서류 합격");
        return application;
    }

    private RecruitingApplication submittedApplication() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            applicationForm(),
            700L,
            200L,
            "identity:1",
            "APP-001",
            "a***@umc.test"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        return application;
    }

    private RecruitingApplicationForm applicationForm() {
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
