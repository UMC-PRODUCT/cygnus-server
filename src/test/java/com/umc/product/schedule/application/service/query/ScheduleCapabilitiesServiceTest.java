package com.umc.product.schedule.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleCapabilitiesService")
class ScheduleCapabilitiesServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    ScheduleCapabilitiesService sut;

    @Test
    @DisplayName("system role SUPER_ADMIN은 challenger 기록 없이 중앙 총괄단 일정 생성 권한을 얻는다")
    void system_super_admin_has_central_core_capabilities() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        ScheduleCapabilitiesInfo result = sut.getCapabilities(MEMBER_ID);

        assertThat(result.canCreateSchedule()).isTrue();
        assertThat(result.canCreateAttendanceRequiredSchedule()).isTrue();
        assertThat(result.maxParticipantCount()).isEqualTo(2000);
        verifyNoInteractions(getChallengerUseCase, getGisuUseCase);
    }
}
