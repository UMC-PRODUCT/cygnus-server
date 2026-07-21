package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
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

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Mock
    SaveRecruitingInterviewSchedulePort saveSchedulePort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Mock
    RecruitingInterviewAvailabilityRequestCoordinator availabilityRequestCoordinator;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingInterviewScheduleCommandService sut;

    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewScheduleCommandService(
            loadSchedulePort,
            saveSchedulePort,
            authorizeManagementUseCase,
            availabilityRequestCoordinator,
            concurrencyLockService
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
    @DisplayName("가능 시간 응답을 기록하고 일정을 확정한다")
    void 가능_시간_응답을_기록하고_일정을_확정한다() {
        RecruitingInterviewSchedule schedule = schedule();
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);

        sut.submitAvailability(SubmitRecruitingInterviewAvailabilityCommand.of(900L, 1L, 700L));
        sut.confirm(ConfirmRecruitingInterviewScheduleCommand.of(
            900L,
            99L,
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "이메일 contact@example.com"
        ));

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.CONFIRMED);
        assertThat(schedule.getContactSnapshot()).isEqualTo("이메일 contact@example.com");
        verify(saveSchedulePort, org.mockito.Mockito.times(2)).saveSchedule(schedule);
    }

    @Test
    @DisplayName("다른 회원은 지원자의 가능 시간 응답 ID를 기록할 수 없다")
    void 다른_회원은_가능_시간_응답을_기록할_수_없다() {
        RecruitingInterviewSchedule schedule = schedule();
        given(loadSchedulePort.getByApplicationId(900L)).willReturn(schedule);

        assertThatThrownBy(() -> sut.submitAvailability(
            SubmitRecruitingInterviewAvailabilityCommand.of(900L, 2L, 700L)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
    }

    private RecruitingInterviewSchedule schedule() {
        return RecruitingInterviewSchedule.requestAvailability(application, "카카오톡 @umc");
    }

    private RecruitingApplication application() {
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
                null,
                "문의 채널"
            )
        );
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
        result.assignInterview(99L, null);
        return result;
    }
}
