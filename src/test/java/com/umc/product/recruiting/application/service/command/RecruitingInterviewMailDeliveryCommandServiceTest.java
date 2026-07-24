package com.umc.product.recruiting.application.service.command;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewMailDeliveryCommandServiceTest {

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;
    @Mock
    SaveRecruitingInterviewSchedulePort saveSchedulePort;
    @Mock
    RecruitingInterviewSchedule schedule;

    RecruitingInterviewMailDeliveryCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewMailDeliveryCommandService(loadSchedulePort, saveSchedulePort);
        given(loadSchedulePort.getByApplicationId(40L)).willReturn(schedule);
    }

    @Test
    @DisplayName("메일 발송 성공은 발송 시각과 상태를 저장한다")
    void 메일_발송_성공은_상태를_저장한다() {
        Instant sentAt = Instant.parse("2026-08-10T00:00:00Z");
        given(schedule.getRequestMailStatus()).willReturn(RecruitingMailDeliveryStatus.PENDING);

        sut.markRequestMailSent(40L, sentAt);

        then(schedule).should().markRequestMailSent(sentAt);
        then(saveSchedulePort).should().saveSchedule(schedule);
    }

    @Test
    @DisplayName("이미 발송된 일정은 실패 이벤트가 뒤늦게 도착해도 상태를 되돌리지 않는다")
    void 이미_발송된_일정은_실패로_되돌리지_않는다() {
        given(schedule.getRequestMailStatus()).willReturn(RecruitingMailDeliveryStatus.SENT);

        sut.markRequestMailFailed(40L, "일시 오류");

        then(schedule).should().getRequestMailStatus();
        then(schedule).shouldHaveNoMoreInteractions();
        then(saveSchedulePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("동일한 실패 결과가 재전달되면 시도 횟수를 중복 증가시키지 않는다")
    void 동일한_실패_결과는_멱등하게_처리한다() {
        given(schedule.getRequestMailStatus()).willReturn(RecruitingMailDeliveryStatus.FAILED);
        given(schedule.getRequestMailError()).willReturn("MessageRejected");

        sut.markRequestMailFailed(40L, "MessageRejected");

        then(schedule).should().getRequestMailStatus();
        then(schedule).should().getRequestMailError();
        then(schedule).shouldHaveNoMoreInteractions();
        then(saveSchedulePort).shouldHaveNoInteractions();
    }
}
