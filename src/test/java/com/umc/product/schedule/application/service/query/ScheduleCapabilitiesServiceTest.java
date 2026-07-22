package com.umc.product.schedule.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
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

    @Test
    @DisplayName("챌린저 활동 이력이 없으면 일정 생성 권한이 없다")
    void member_without_challenger_history_is_not_allowed() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of());

        ScheduleCapabilitiesInfo result = sut.getCapabilities(MEMBER_ID);

        assertThat(result.canCreateSchedule()).isFalse();
        assertThat(result.maxParticipantCount()).isZero();
        verifyNoInteractions(getGisuUseCase);
    }

    @Test
    @DisplayName("활성 기수 역할이 없으면 일반 챌린저 제한을 적용한다")
    void challenger_without_current_role_uses_default_limit() {
        prepareActiveChallenger();
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            role(99L, ChallengerRoleType.CENTRAL_PRESIDENT)
        ));

        ScheduleCapabilitiesInfo result = sut.getCapabilities(MEMBER_ID);

        assertThat(result.canCreateSchedule()).isTrue();
        assertThat(result.canCreateAttendanceRequiredSchedule()).isFalse();
        assertThat(result.maxParticipantCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("현재 기수의 가장 높은 역할을 기준으로 모든 role tier를 매핑한다")
    void maps_every_role_tier() {
        assertRoleLimit(ChallengerRoleType.CENTRAL_PRESIDENT, 2_000);
        assertRoleLimit(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, 300);
        assertRoleLimit(ChallengerRoleType.CHAPTER_PRESIDENT, 300);
        assertRoleLimit(ChallengerRoleType.SCHOOL_PRESIDENT, 100);
        assertRoleLimit(ChallengerRoleType.SCHOOL_PART_LEADER, 100);
    }

    @Test
    @DisplayName("여러 역할이 있으면 입력 순서와 무관하게 가장 높은 권한을 선택한다")
    void selects_highest_role_independent_of_order() {
        prepareActiveChallenger();
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            role(1L, ChallengerRoleType.SCHOOL_PART_LEADER),
            role(1L, ChallengerRoleType.CENTRAL_VICE_PRESIDENT),
            role(1L, ChallengerRoleType.CHAPTER_PRESIDENT)
        ));

        assertThat(sut.getCapabilities(MEMBER_ID).maxParticipantCount()).isEqualTo(2_000);
    }

    private void assertRoleLimit(ChallengerRoleType roleType, int expectedLimit) {
        org.mockito.Mockito.reset(getChallengerUseCase, getGisuUseCase, getChallengerRoleUseCase);
        prepareActiveChallenger();
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(role(1L, roleType)));

        ScheduleCapabilitiesInfo result = sut.getCapabilities(MEMBER_ID);

        assertThat(result.maxParticipantCount()).isEqualTo(expectedLimit);
        assertThat(result.canCreateAttendanceRequiredSchedule()).isTrue();
    }

    private void prepareActiveChallenger() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID))
            .willReturn(List.of(ChallengerInfo.builder().memberId(MEMBER_ID).gisuId(1L).build()));
        given(getGisuUseCase.getActiveGisu()).willReturn(
            new GisuInfo(1L, 1L, Instant.EPOCH, Instant.parse("2030-01-01T00:00:00Z"), true)
        );
    }

    private ChallengerRoleInfo role(Long gisuId, ChallengerRoleType roleType) {
        return ChallengerRoleInfo.builder().gisuId(gisuId).roleType(roleType).build();
    }
}
