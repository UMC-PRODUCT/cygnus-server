package com.umc.product.schedule.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupScheduleUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleQueryService")
class ScheduleQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long ACTIVE_GISU_ID = 7L;
    private static final Long AUTHORED_SCHEDULE_ID = 20L;
    private static final Long STUDY_GROUP_SCHEDULE_ID = 30L;

    @Mock
    LoadSchedulePort loadSchedulePort;

    @Mock
    LoadScheduleParticipantPort loadScheduleParticipantPort;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetStudyGroupScheduleUseCase getStudyGroupScheduleUseCase;

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

    @Test
    @DisplayName("학교 회장단은 본인 생성 일정과 스터디 그룹 일정을 합쳐서 조회 범위로 삼는다")
    void schoolCoreScopeIncludesStudyGroupSchedules() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_PRESIDENT);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(AUTHORED_SCHEDULE_ID));
        given(getStudyGroupScheduleUseCase.findVisibleScheduleIdsByMemberId(MEMBER_ID))
            .willReturn(Set.of(STUDY_GROUP_SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(captureTargetScheduleIds())
            .containsExactlyInAnyOrder(AUTHORED_SCHEDULE_ID, STUDY_GROUP_SCHEDULE_ID);
    }

    @Test
    @DisplayName("교내 파트장은 본인 생성 일정과 본인 멘토 스터디 그룹 일정을 조회 범위로 삼는다")
    void partLeaderScopeIncludesStudyGroupSchedules() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_PART_LEADER);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(AUTHORED_SCHEDULE_ID));
        given(getStudyGroupScheduleUseCase.findVisibleScheduleIdsByMemberId(MEMBER_ID))
            .willReturn(Set.of(STUDY_GROUP_SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(captureTargetScheduleIds())
            .containsExactlyInAnyOrder(AUTHORED_SCHEDULE_ID, STUDY_GROUP_SCHEDULE_ID);
    }

    @Test
    @DisplayName("회장단과 파트장을 겸직해도 스터디 그룹 일정은 한 번만 조회한다")
    void studyGroupSchedulesAreQueriedOnceWhenMemberHoldsBothRoles() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_PRESIDENT, ChallengerRoleType.SCHOOL_PART_LEADER);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(AUTHORED_SCHEDULE_ID));
        given(getStudyGroupScheduleUseCase.findVisibleScheduleIdsByMemberId(MEMBER_ID))
            .willReturn(Set.of(STUDY_GROUP_SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        then(getStudyGroupScheduleUseCase).should(times(1)).findVisibleScheduleIdsByMemberId(MEMBER_ID);
        assertThat(captureTargetScheduleIds())
            .containsExactlyInAnyOrder(AUTHORED_SCHEDULE_ID, STUDY_GROUP_SCHEDULE_ID);
    }

    @Test
    @DisplayName("기타 교내 운영진은 본인 생성 일정만 조회하고 스터디 그룹 일정은 조회하지 않는다")
    void schoolEtcAdminScopeExcludesStudyGroupSchedules() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_ETC_ADMIN);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(AUTHORED_SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(captureTargetScheduleIds()).containsExactly(AUTHORED_SCHEDULE_ID);
        verifyNoInteractions(getStudyGroupScheduleUseCase);
    }

    @Test
    @DisplayName("중앙 운영진은 본인 참석 일정만 조회하고 스터디 그룹 일정은 조회하지 않는다")
    void centralMemberScopeExcludesStudyGroupSchedules() {
        givenActiveGisuRoles(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER);
        given(loadScheduleParticipantPort.findScheduleIdsByMemberId(MEMBER_ID)).willReturn(Set.of(SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(captureTargetScheduleIds()).containsExactly(SCHEDULE_ID);
        verifyNoInteractions(getStudyGroupScheduleUseCase);
    }

    @Test
    @DisplayName("보이는 스터디 그룹 일정이 없는 회장단은 본인 생성 일정만 조회 범위로 삼는다")
    void schoolCoreWithoutVisibleStudyGroupSchedulesKeepsAuthoredOnly() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_PRESIDENT);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(AUTHORED_SCHEDULE_ID));
        given(getStudyGroupScheduleUseCase.findVisibleScheduleIdsByMemberId(MEMBER_ID)).willReturn(Set.of());
        given(loadSchedulePort.findAdminSchedulesByRole(any(), isNull(), isNull(), isNull()))
            .willReturn(List.of());

        sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(captureTargetScheduleIds()).containsExactly(AUTHORED_SCHEDULE_ID);
    }

    @Test
    @DisplayName("조회 대상 일정이 하나도 없으면 일정 조회 없이 빈 목록을 반환한다")
    void emptyTargetScopeSkipsScheduleQuery() {
        givenActiveGisuRoles(ChallengerRoleType.SCHOOL_PRESIDENT);
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of());
        given(getStudyGroupScheduleUseCase.findVisibleScheduleIdsByMemberId(MEMBER_ID)).willReturn(Set.of());

        List<AdminScheduleInfo> result = sut.searchAdminSchedules(null, null, null, MEMBER_ID);

        assertThat(result).isEmpty();
        then(loadSchedulePort).should(never()).findAdminSchedulesByRole(any(), any(), any(), any());
    }

    // ========== Helper Methods ==========

    // 활성 기수에서 주어진 역할들을 가진 사용자로 세팅
    private void givenActiveGisuRoles(ChallengerRoleType... roleTypes) {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getGisuUseCase.getActiveGisu()).willReturn(activeGisu());
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(Arrays.stream(roleTypes).map(ScheduleQueryServiceTest::roleInfo).toList());
    }

    // findAdminSchedulesByRole 에 실제로 넘어간 조회 대상 일정 ID 집합
    @SuppressWarnings("unchecked")
    private Set<Long> captureTargetScheduleIds() {
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        then(loadSchedulePort).should()
            .findAdminSchedulesByRole(captor.capture(), isNull(), isNull(), isNull());
        return captor.getValue();
    }

    private static GisuInfo activeGisu() {
        return new GisuInfo(ACTIVE_GISU_ID, 7L, null, null, true);
    }

    private static ChallengerRoleInfo roleInfo(ChallengerRoleType roleType) {
        return ChallengerRoleInfo.builder()
            .roleType(roleType)
            .gisuId(ACTIVE_GISU_ID)
            .build();
    }
}
