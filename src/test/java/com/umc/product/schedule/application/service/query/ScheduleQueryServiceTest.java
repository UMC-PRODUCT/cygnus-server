package com.umc.product.schedule.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleQueryService")
class ScheduleQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @Mock
    LoadSchedulePort loadSchedulePort;

    @Mock
    LoadScheduleParticipantPort loadScheduleParticipantPort;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    ScheduleQueryService sut;

    @Test
    @DisplayName("system role SUPER_ADMIN은 challenger role 없이 본인 참여 일정의 운영진 조회 범위를 얻는다")
    void system_super_admin_uses_participant_schedule_scope() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);
        given(loadScheduleParticipantPort.findScheduleIdsByMemberId(MEMBER_ID)).willReturn(Set.of(SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(Set.of(SCHEDULE_ID), null, null, null))
            .willReturn(List.of());

        List<AdminScheduleInfo> result = sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(result).isEmpty();
        then(loadSchedulePort).should().findAdminSchedulesByRole(
            eq(Set.of(SCHEDULE_ID)), isNull(), isNull(), isNull());
        verifyNoInteractions(getGisuUseCase);
    }
}
