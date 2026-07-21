package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewScheduleQueryServiceTest {

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @InjectMocks
    RecruitingInterviewScheduleQueryService sut;

    @Test
    @DisplayName("지원서 면접 일정이 있으면 기본 정보를 조회한다")
    void 지원서_면접_일정이_있으면_기본_정보를_조회한다() {
        RecruitingApplication application = org.mockito.Mockito.mock(RecruitingApplication.class);
        given(application.getId()).willReturn(900L);
        given(application.getApplicantMemberId()).willReturn(1L);
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            application,
            "카카오톡 @umc"
        );
        ReflectionTestUtils.setField(schedule, "id", 1000L);
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.of(schedule));

        var result = sut.findByApplicationId(900L, 1L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().id()).isEqualTo(1000L);
        assertThat(result.orElseThrow().status())
            .isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
    }

    @Test
    @DisplayName("지원서 면접 일정이 없으면 빈 결과를 반환한다")
    void 지원서_면접_일정이_없으면_빈_결과를_반환한다() {
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.empty());

        assertThat(sut.findByApplicationId(900L, 1L)).isEmpty();
    }
}
