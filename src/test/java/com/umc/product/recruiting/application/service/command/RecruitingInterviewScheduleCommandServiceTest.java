package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewScheduleCommandServiceTest {

    private static final Instant INTERVIEW_START_AT = Instant.parse("2026-08-11T00:00:00Z");

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Mock
    LoadRecruitingInterviewSessionPort loadSessionPort;

    @Mock
    SaveRecruitingInterviewSchedulePort saveSchedulePort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Mock
    RecruitingInterviewAvailabilityRequestCoordinator availabilityRequestCoordinator;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    @Mock
    GetFormUseCase getFormUseCase;

    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;

    @Mock
    ConfirmRecruitingInterviewSchedulesUseCase confirmSchedulesUseCase;

    RecruitingInterviewScheduleCommandService sut;

    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewScheduleCommandService(
            loadSchedulePort,
            saveSchedulePort,
            authorizeManagementUseCase,
            availabilityRequestCoordinator,
            concurrencyLockService,
            getFormUseCase,
            manageFormResponseUseCase,
            loadSessionPort,
            confirmSchedulesUseCase
        );
        application = application();
    }

    @Test
    @DisplayName("수동 면접 가능 시간 요청은 멱등 coordinator에 위임한다")
    void 수동_면접_가능_시간_요청은_멱등_coordinator에_위임한다() {
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);
        RecruitingInterviewSchedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", 1000L);
        given(availabilityRequestCoordinator.request(application, "카카오톡 @umc")).willReturn(schedule);

        Long scheduleId = sut.requestAvailability(
            RequestRecruitingInterviewScheduleCommand.of(900L, 99L, "카카오톡 @umc")
        );

        assertThat(scheduleId).isEqualTo(1000L);
        verify(availabilityRequestCoordinator).request(application, "카카오톡 @umc");
        verify(authorizeManagementUseCase).authorizeSeasonManagement(99L, 700L);
    }

    @Test
    @DisplayName("지원자는 요청된 면접 가능 시간을 제출한다")
    void 지원자는_요청된_면접_가능_시간을_제출한다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        List<Instant> times = List.of(
            Instant.parse("2026-08-11T00:00:00Z"),
            Instant.parse("2026-08-14T23:45:00Z")
        );
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(availabilityForm());
        given(manageFormResponseUseCase.submitImmediately(org.mockito.ArgumentMatchers.any()))
            .willReturn(700L);

        sut.submitAvailability(SubmitRecruitingInterviewAvailabilityCommand.of(900L, 1L, times));

        ArgumentCaptor<SubmitFormResponseCommand> captor = ArgumentCaptor.forClass(SubmitFormResponseCommand.class);
        verify(manageFormResponseUseCase).submitImmediately(captor.capture());
        SubmitFormResponseCommand submitted = captor.getValue();
        assertThat(submitted.formId()).isEqualTo(300L);
        assertThat(submitted.respondentMemberId()).isEqualTo(1L);
        assertThat(submitted.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.questionId()).isEqualTo(301L);
            assertThat(answer.times()).containsExactlyElementsOf(times);
            assertThat(answer.textValue()).isNull();
            assertThat(answer.selectedOptionIds()).isNull();
            assertThat(answer.fileIds()).isNull();
        });
        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED);
        assertThat(schedule.getAvailabilityFormResponseId()).isEqualTo(700L);
        verify(saveSchedulePort).saveSchedule(schedule);
    }

    @Test
    @DisplayName("면접 가능 시간 command는 입력 목록을 방어적으로 복사한다")
    void 면접_가능_시간_command는_입력_목록을_방어적으로_복사한다() {
        List<Instant> mutableTimes = new ArrayList<>(List.of(INTERVIEW_START_AT));
        SubmitRecruitingInterviewAvailabilityCommand command =
            SubmitRecruitingInterviewAvailabilityCommand.of(900L, 1L, mutableTimes);

        mutableTimes.clear();

        assertThat(command.times()).containsExactly(INTERVIEW_START_AT);
        assertThatThrownBy(() -> command.times().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("지원자가 아니면 일정과 Form을 조회하지 않는다")
    void 지원자가_아니면_일정과_Form을_조회하지_않는다() {
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);

        assertRecruitingError(
            () -> sut.submitAvailability(command(2L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH
        );

        verifyNoInteractions(loadSchedulePort, getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("면접 배정 상태가 아니면 일정과 Form을 조회하지 않는다")
    void 면접_배정_상태가_아니면_일정과_Form을_조회하지_않는다() {
        RecruitingApplication submitted = application(false);
        given(concurrencyLockService.lockApplication(900L)).willReturn(submitted);

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION
        );

        verifyNoInteractions(loadSchedulePort, getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("일정이 요청 상태가 아니면 Form을 호출하지 않는다")
    void 일정이_요청_상태가_아니면_Form을_호출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        schedule.submitAvailability(700L);

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION
        );

        verifyNoInteractions(getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("면접 가능 시간 요청 일정이 없으면 Form을 호출하지 않는다")
    void 면접_가능_시간_요청_일정이_없으면_Form을_호출하지_않는다() {
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);
        given(loadSchedulePort.getByApplicationId(900L)).willThrow(
            new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID)
        );

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID
        );

        verifyNoInteractions(getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("요청 전 상태의 일정에는 가능 시간을 제출할 수 없다")
    void 요청_전_상태의_일정에는_가능_시간을_제출할_수_없다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        ReflectionTestUtils.setField(schedule, "status", null);

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION
        );

        verifyNoInteractions(getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("취소된 일정에는 가능 시간을 제출할 수 없다")
    void 취소된_일정에는_가능_시간을_제출할_수_없다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        schedule.cancel();

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION
        );

        verifyNoInteractions(getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("Form 매핑이 누락되면 Form을 호출하지 않는다")
    void Form_매핑이_누락되면_Form을_호출하지_않는다() {
        givenRequestedSchedule();
        ReflectionTestUtils.setField(application.getRound(), "availabilityScheduleQuestionId", null);

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-12T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE
        );

        verifyNoInteractions(getFormUseCase, manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("발행되지 않은 Form은 제출하지 않는다")
    void 발행되지_않은_Form은_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.DRAFT, 301L, QuestionType.SCHEDULE, true, false, false)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("지정 질문이 Form에 없으면 제출하지 않는다")
    void 지정_질문이_Form에_없으면_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.PUBLISHED, 999L, QuestionType.SCHEDULE, true, false, false)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("지정 질문이 SCHEDULE 유형이 아니면 제출하지 않는다")
    void 지정_질문이_SCHEDULE_유형이_아니면_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.PUBLISHED, 301L, QuestionType.SHORT_TEXT, true, false, false)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("지정 SCHEDULE 질문이 필수가 아니면 제출하지 않는다")
    void 지정_SCHEDULE_질문이_필수가_아니면_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.PUBLISHED, 301L, QuestionType.SCHEDULE, false, false, false)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("다른 필수 질문이 있으면 제출하지 않는다")
    void 다른_필수_질문이_있으면_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.PUBLISHED, 301L, QuestionType.SCHEDULE, true, true, false)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("익명 Form에는 기명 면접 가능 시간을 제출하지 않는다")
    void 익명_Form에는_기명_면접_가능_시간을_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(
            availabilityForm(FormStatus.PUBLISHED, 301L, QuestionType.SCHEDULE, true, false, true)
        );

        assertInvalidFormContract(schedule);
    }

    @Test
    @DisplayName("면접 시작 경계는 포함하고 종료 경계는 제외한다")
    void 면접_시작_경계는_포함하고_종료_경계는_제외한다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(availabilityForm());

        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-15T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD
        );

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        verifyNoInteractions(manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("면접 시작 전 시간 하나라도 포함되면 제출하지 않는다")
    void 면접_시작_전_시간_하나라도_포함되면_제출하지_않는다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(availabilityForm());

        assertRecruitingError(
            () -> sut.submitAvailability(SubmitRecruitingInterviewAvailabilityCommand.of(
                900L,
                1L,
                List.of(
                    Instant.parse("2026-08-11T00:00:00Z"),
                    Instant.parse("2026-08-10T23:59:59Z")
                )
            )),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD
        );

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        verifyNoInteractions(manageFormResponseUseCase, saveSchedulePort);
    }

    @Test
    @DisplayName("Form이 15분 미정렬 시간을 거부하면 전달값을 보존하고 일정 상태를 유지한다")
    void Form이_15분_미정렬_시간을_거부하면_전달값을_보존하고_일정_상태를_유지한다() {
        RecruitingInterviewSchedule schedule = givenRequestedSchedule();
        Instant misalignedTime = Instant.parse("2026-08-12T10:01:00Z");
        given(getFormUseCase.getFormWithStructure(300L)).willReturn(availabilityForm());
        given(manageFormResponseUseCase.submitImmediately(org.mockito.ArgumentMatchers.any()))
            .willAnswer(invocation -> {
                SubmitFormResponseCommand submitted = invocation.getArgument(0);
                assertThat(submitted.formId()).isEqualTo(300L);
                assertThat(submitted.respondentMemberId()).isEqualTo(1L);
                assertThat(submitted.answers()).singleElement().satisfies(answer -> {
                    assertThat(answer.questionId()).isEqualTo(301L);
                    assertThat(answer.times()).containsExactly(misalignedTime);
                });
                throw new FormDomainException(FormErrorCode.INVALID_ANSWER_FORMAT);
            });

        assertThatThrownBy(() -> sut.submitAvailability(
            SubmitRecruitingInterviewAvailabilityCommand.of(900L, 1L, List.of(misalignedTime))
        ))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_ANSWER_FORMAT);

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        assertThat(schedule.getAvailabilityFormResponseId()).isNull();
        verify(saveSchedulePort, never()).saveSchedule(schedule);
    }

    @Test
    @DisplayName("세션 값과 일치하는 단건 확정은 공통 batch 확정에 요청 슬롯으로 위임한다")
    void 세션_값과_일치하는_단건_확정은_공통_batch_확정에_요청_슬롯으로_위임한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);
        givenSessionValues();

        sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
            900L,
            99L,
            101L,
            sessionStartsAt(),
            sessionEndsAt(),
            "서버 회의실",
            "이메일 contact@example.com"
        ));

        ArgumentCaptor<com.umc.product.recruiting.application.port.in.command.dto
            .ConfirmRecruitingInterviewSchedulesCommand> captor = ArgumentCaptor.forClass(
                com.umc.product.recruiting.application.port.in.command.dto
                    .ConfirmRecruitingInterviewSchedulesCommand.class
            );
        verify(confirmSchedulesUseCase).confirmAll(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(800L);
        assertThat(captor.getValue().assignments()).singleElement().satisfies(assignment -> {
            assertThat(assignment.applicationId()).isEqualTo(900L);
            assertThat(assignment.sessionId()).isEqualTo(101L);
            assertThat(assignment.startsAt()).isEqualTo(sessionStartsAt());
            assertThat(assignment.contactSnapshot()).isEqualTo("이메일 contact@example.com");
        });
        verifyNoInteractions(saveSchedulePort);
    }

    @Test
    @DisplayName("단건 확정은 세션을 조회하기 전에 지원서 Season 관리 권한을 확인한다")
    void 단건_확정은_세션을_조회하기_전에_지원서_Season_관리_권한을_확인한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);
        givenSessionValues();

        sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
            900L, 99L, 101L, sessionStartsAt(), sessionEndsAt(), "서버 회의실", "운영진 연락처"
        ));

        InOrder order = org.mockito.Mockito.inOrder(loadSchedulePort, authorizeManagementUseCase, loadSessionPort);
        order.verify(loadSchedulePort).getByApplicationId(900L);
        order.verify(authorizeManagementUseCase).authorizeSeasonManagement(99L, 700L);
        order.verify(loadSessionPort).getById(101L);
    }

    @Test
    @DisplayName("단건 확정 권한이 없으면 세션을 조회하지 않는다")
    void 단건_확정_권한이_없으면_세션을_조회하지_않는다() {
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("권한 없음"))
            .given(authorizeManagementUseCase)
            .authorizeSeasonManagement(99L, 700L);

        assertThatThrownBy(() -> sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
            900L, 99L, 101L, sessionStartsAt(), sessionEndsAt(), "서버 회의실", "운영진 연락처"
        ))).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(loadSessionPort, confirmSchedulesUseCase);
    }

    @Test
    @DisplayName("단건 확정은 세션 안의 다음 슬롯도 공통 batch 확정에 위임한다")
    void 단건_확정은_세션_안의_다음_슬롯도_공통_batch_확정에_위임한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);
        givenSessionValues();

        Instant startsAt = sessionStartsAt().plusSeconds(900);
        sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
            900L,
            99L,
            101L,
            startsAt,
            startsAt.plusSeconds(1800),
            "서버 회의실",
            "이메일 contact@example.com"
        ));

        ArgumentCaptor<com.umc.product.recruiting.application.port.in.command.dto
            .ConfirmRecruitingInterviewSchedulesCommand> captor = ArgumentCaptor.forClass(
                com.umc.product.recruiting.application.port.in.command.dto
                    .ConfirmRecruitingInterviewSchedulesCommand.class
            );
        verify(confirmSchedulesUseCase).confirmAll(captor.capture());
        assertThat(captor.getValue().assignments()).singleElement().satisfies(assignment ->
            assertThat(assignment.startsAt()).isEqualTo(startsAt)
        );
    }

    @Test
    @DisplayName("단건 확정 요청의 종료 시각이 세션 슬롯과 다르면 충돌로 거부한다")
    void 단건_확정_요청의_종료_시각이_세션_슬롯과_다르면_충돌로_거부한다() {
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule());
        givenSessionValues();

        assertRecruitingError(
            () -> sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
                900L,
                99L,
                101L,
                sessionStartsAt(),
                sessionEndsAt().plusSeconds(900),
                "서버 회의실",
                "이메일 contact@example.com"
            )),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );

        verifyNoInteractions(confirmSchedulesUseCase);
    }

    @Test
    @DisplayName("단건 확정 요청의 장소가 세션과 다르면 충돌로 거부한다")
    void 단건_확정_요청의_장소가_세션과_다르면_충돌로_거부한다() {
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule());
        givenSessionValues();

        assertRecruitingError(
            () -> sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
                900L,
                99L,
                101L,
                sessionStartsAt(),
                sessionEndsAt(),
                "클라이언트 장소",
                "이메일 contact@example.com"
            )),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );

        verifyNoInteractions(confirmSchedulesUseCase);
    }

    private RecruitingInterviewSchedule schedule() {
        return RecruitingInterviewSchedule.requestAvailability(application, "카카오톡 @umc");
    }

    private void givenSessionValues() {
        com.umc.product.recruiting.domain.RecruitingInterviewSession session = org.mockito.Mockito.mock(
            com.umc.product.recruiting.domain.RecruitingInterviewSession.class
        );
        given(loadSessionPort.getById(101L)).willReturn(session);
        org.mockito.Mockito.lenient().when(session.getStartsAt()).thenReturn(sessionStartsAt());
        org.mockito.Mockito.lenient().when(session.getSlotDurationMinutes()).thenReturn(30);
        org.mockito.Mockito.lenient().when(session.getLocation()).thenReturn("서버 회의실");
    }

    private Instant sessionStartsAt() {
        return Instant.parse("2026-08-12T01:00:00Z");
    }

    private Instant sessionEndsAt() {
        return Instant.parse("2026-08-12T01:30:00Z");
    }

    private RecruitingInterviewSchedule givenRequestedSchedule() {
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);
        RecruitingInterviewSchedule schedule = schedule();
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);
        return schedule;
    }

    private SubmitRecruitingInterviewAvailabilityCommand command(Long requesterMemberId, Instant time) {
        return SubmitRecruitingInterviewAvailabilityCommand.of(900L, requesterMemberId, List.of(time));
    }

    private FormWithStructureInfo availabilityForm() {
        return availabilityForm(FormStatus.PUBLISHED, 301L, QuestionType.SCHEDULE, true, false, false);
    }

    private FormWithStructureInfo availabilityForm(
        FormStatus status,
        Long questionId,
        QuestionType questionType,
        boolean required,
        boolean extraRequired,
        boolean anonymous
    ) {
        List<FormWithStructureInfo.QuestionWithOptions> questions = new java.util.ArrayList<>();
        questions.add(FormWithStructureInfo.QuestionWithOptions.builder()
            .questionId(questionId)
            .type(questionType)
            .isRequired(required)
            .options(List.of())
            .build());
        if (extraRequired) {
            questions.add(FormWithStructureInfo.QuestionWithOptions.builder()
                .questionId(302L)
                .type(QuestionType.SHORT_TEXT)
                .isRequired(true)
                .options(List.of())
                .build());
        }
        return FormWithStructureInfo.builder()
            .formId(300L)
            .status(status)
            .isAnonymous(anonymous)
            .sections(List.of(FormWithStructureInfo.SectionWithQuestions.builder()
                .sectionId(400L)
                .questions(List.copyOf(questions))
                .build()))
            .build();
    }

    private void assertInvalidFormContract(RecruitingInterviewSchedule schedule) {
        assertRecruitingError(
            () -> sut.submitAvailability(command(1L, Instant.parse("2026-08-11T00:00:00Z"))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE
        );
        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        assertThat(schedule.getAvailabilityFormResponseId()).isNull();
        verifyNoInteractions(manageFormResponseUseCase, saveSchedulePort);
    }

    private void assertRecruitingError(Runnable action, RecruitingErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(expected);
    }

    private RecruitingApplication application() {
        return application(true);
    }

    private RecruitingApplication application(boolean interviewAssigned) {
        RecruitingSeason season = RecruitingSeason.create(9L, 1L);
        ReflectionTestUtils.setField(season, "id", 700L);
        RecruitingRound round = RecruitingRound.createRegular(
            season,
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                true,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-08-16T00:00:00Z"),
                300L,
                301L,
                null,
                "문의 채널"
            )
        );
        ReflectionTestUtils.setField(round, "id", 800L);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 100L);
        RecruitingApplication result = RecruitingApplication.createMemberDraft(
            form,
            200L,
            1L,
            RecruitingApplicantProfile.create(
                round,
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(result, "id", 900L);
        result.submit(1L);
        if (interviewAssigned) {
            result.assignInterview(99L, null);
        }
        return result;
    }
}
