package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.recruiting.application.event.InterviewAvailabilityRequestedEvent;
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
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewAvailabilityRequestCoordinatorTest {

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;
    @Mock
    SaveRecruitingInterviewSchedulePort saveSchedulePort;
    @Mock
    DomainEventPublisher eventPublisher;

    RecruitingInterviewAvailabilityRequestCoordinator sut;
    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewAvailabilityRequestCoordinator(
            loadSchedulePort,
            saveSchedulePort,
            eventPublisher
        );
        application = application();
    }

    @Test
    @DisplayName("최초 요청은 일정 row와 Outbox 이벤트를 함께 생성한다")
    void 최초_요청은_일정_row와_Outbox_이벤트를_함께_생성한다() {
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.empty());
        given(saveSchedulePort.saveSchedule(any())).willAnswer(invocation -> {
            RecruitingInterviewSchedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 1000L);
            return schedule;
        });

        RecruitingInterviewSchedule result = sut.request(application, "문의: recruit@example.org");

        assertThat(result.getId()).isEqualTo(1000L);
        assertThat(result.getRequestMailStatus()).isEqualTo(RecruitingMailDeliveryStatus.PENDING);
        ArgumentCaptor<InterviewAvailabilityRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(InterviewAvailabilityRequestedEvent.class);
        then(eventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().applicationId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("PENDING 또는 SENT 요청은 기존 일정을 반환하고 이벤트를 중복 발행하지 않는다")
    void 진행_중이거나_발송된_요청은_멱등_성공한다() {
        RecruitingInterviewSchedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", 1000L);
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.of(schedule));

        RecruitingInterviewSchedule result = sut.request(application, "새 연락처");

        assertThat(result).isSameAs(schedule);
        then(saveSchedulePort).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("FAILED 요청은 PENDING으로 바꾸고 재시도 이벤트를 발행한다")
    void 실패한_요청은_재시도_이벤트를_발행한다() {
        RecruitingInterviewSchedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", 1000L);
        schedule.markRequestMailFailed("일시 오류");
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.of(schedule));
        given(saveSchedulePort.saveSchedule(schedule)).willReturn(schedule);

        RecruitingInterviewSchedule result = sut.request(application, "새 연락처");

        assertThat(result.getRequestMailStatus()).isEqualTo(RecruitingMailDeliveryStatus.PENDING);
        then(saveSchedulePort).should().saveSchedule(schedule);
        then(eventPublisher).should().publish(any(InterviewAvailabilityRequestedEvent.class));
    }

    private RecruitingInterviewSchedule schedule() {
        return RecruitingInterviewSchedule.requestAvailability(application, "문의: recruit@example.org");
    }

    private RecruitingApplication application() {
        RecruitingSeason season = RecruitingSeason.create(9L, 1L);
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
                "문의: recruit@example.org"
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
        return result;
    }
}
