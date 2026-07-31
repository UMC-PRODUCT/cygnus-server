package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewMailDeliveryQueryServiceTest {

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;
    @Mock
    RecruitingInterviewSchedule schedule;
    @Mock
    RecruitingApplication application;
    @Mock
    RecruitingRound round;
    @InjectMocks
    RecruitingInterviewMailDeliveryQueryService sut;

    @Test
    @DisplayName("면접 일정 요청 메일 정보는 지원서와 일정 스냅샷에서 조회한다")
    void 면접_일정_요청_메일_정보를_조회한다() {
        given(loadSchedulePort.getByApplicationId(40L)).willReturn(schedule);
        given(schedule.getApplication()).willReturn(application);
        given(application.getId()).willReturn(40L);
        given(application.getApplicantEmail()).willReturn("applicant@example.com");
        given(application.getApplicantName()).willReturn("지원자");
        given(application.getRound()).willReturn(round);
        given(round.getAvailabilityFormId()).willReturn(300L);
        given(schedule.getContactSnapshot()).willReturn("문의 채널");
        given(schedule.getRequestMailStatus()).willReturn(RecruitingMailDeliveryStatus.PENDING);

        RecruitingInterviewRequestMailInfo result = sut.getRequestMail(40L);

        assertThat(result.recipientEmail()).isEqualTo("applicant@example.com");
        assertThat(result.availabilityFormId()).isEqualTo(300L);
        assertThat(result.contactText()).isEqualTo("문의 채널");
        assertThat(result.isSent()).isFalse();
    }
}
