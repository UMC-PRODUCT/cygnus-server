package com.umc.product.schedule.application.service.query;

import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleInfo;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.dto.ScheduleParticipantDetailDto;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

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

    @Test
    @DisplayName("내 일정이 없으면 참여자 상세 조회를 생략한다")
    void empty_my_schedules_short_circuit() {
        given(loadSchedulePort.findMySchedules(MEMBER_ID, null, null, null)).willReturn(List.of());

        assertThat(sut.searchMySchedules(null, null, null, MEMBER_ID)).isEmpty();

        then(loadScheduleParticipantPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내 일정을 batch 참여자 map과 결합하고 참여자가 없는 일정은 빈 목록을 사용한다")
    void search_my_schedules_uses_batch_participants() {
        Schedule first = identifiedSchedule(10L);
        Schedule second = identifiedSchedule(11L);
        Instant from = Instant.parse("2029-01-01T00:00:00Z");
        Instant to = Instant.parse("2031-01-01T00:00:00Z");
        given(loadSchedulePort.findMySchedules(MEMBER_ID, from, to, true)).willReturn(List.of(first, second));
        given(loadScheduleParticipantPort.findParticipantDetailsByScheduleIds(List.of(10L, 11L)))
            .willReturn(List.of(detail(10L, MEMBER_ID, AttendanceStatus.PRESENT)));

        List<ScheduleInfo> result = sut.searchMySchedules(from, to, true, MEMBER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isParticipant()).isTrue();
        assertThat(result.get(1).participants()).isEmpty();
    }

    @Test
    @DisplayName("일정 상세는 tag 포함 일정과 참여자 상세를 결합한다")
    void gets_schedule_details() {
        Schedule schedule = identifiedSchedule(SCHEDULE_ID);
        given(loadSchedulePort.findByIdWithTags(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findParticipantDetailsByScheduleId(SCHEDULE_ID))
            .willReturn(List.of(detail(SCHEDULE_ID, MEMBER_ID, AttendanceStatus.LATE)));

        ScheduleInfo result = sut.getScheduleDetails(SCHEDULE_ID, MEMBER_ID);

        assertThat(result.attendanceStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(result.participants()).hasSize(1);
    }

    @Test
    @DisplayName("일정 상세와 운영진 상세는 존재하지 않는 일정을 구분한다")
    void detail_not_found() {
        given(loadSchedulePort.findByIdWithTags(SCHEDULE_ID)).willReturn(Optional.empty());

        assertError(() -> sut.getScheduleDetails(SCHEDULE_ID, MEMBER_ID), ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> sut.getAdminSchedule(SCHEDULE_ID, MEMBER_ID, null),
            ScheduleErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("운영진 역할별 일정 범위를 합집합으로 모으고 참여자 상태를 필터링한다")
    void search_admin_schedules_collects_role_union() {
        prepareCurrentRoles(List.of(
            role(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER),
            role(ChallengerRoleType.SCHOOL_PRESIDENT),
            role(ChallengerRoleType.SCHOOL_PART_LEADER),
            role(ChallengerRoleType.SCHOOL_ETC_ADMIN)
        ));
        given(loadScheduleParticipantPort.findScheduleIdsByMemberId(MEMBER_ID)).willReturn(Set.of(10L));
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(11L));
        Schedule first = identifiedSchedule(10L);
        Schedule second = identifiedSchedule(11L);
        given(loadSchedulePort.findAdminSchedulesByRole(Set.of(10L, 11L), null, null,
            AttendanceStatus.PRESENT_PENDING)).willReturn(List.of(first, second));
        given(loadScheduleParticipantPort.findParticipantDetailsByScheduleIds(List.of(10L, 11L)))
            .willReturn(List.of(
                detail(10L, 2L, AttendanceStatus.PRESENT_PENDING),
                detail(10L, 3L, AttendanceStatus.ABSENT)
            ));

        List<AdminScheduleInfo> result = sut.searchAdminSchedules(
            null, null, AttendanceStatus.PRESENT_PENDING, MEMBER_ID
        );

        assertThat(result).singleElement().satisfies(info ->
            assertThat(info.participants()).singleElement()
                .extracting(AdminScheduleInfo.AdminScheduleParticipantInfo::attendanceStatus)
                .isEqualTo(AttendanceStatus.PRESENT_PENDING)
        );
    }

    @Test
    @DisplayName("운영진 역할이나 대상 일정이 없으면 일정 query를 생략한다")
    void empty_admin_scope_short_circuit() {
        prepareCurrentRoles(List.of());

        assertThat(sut.searchAdminSchedules(null, null, null, MEMBER_ID)).isEmpty();

        then(loadSchedulePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회 범위는 있지만 조건에 맞는 일정이 없으면 참여자를 조회하지 않는다")
    void no_admin_schedule_short_circuit() {
        prepareCurrentRoles(List.of(role(ChallengerRoleType.SCHOOL_PART_LEADER)));
        given(loadSchedulePort.findScheduleIdsByAuthor(MEMBER_ID)).willReturn(Set.of(SCHEDULE_ID));
        given(loadSchedulePort.findAdminSchedulesByRole(Set.of(SCHEDULE_ID), null, null, null))
            .willReturn(List.of());

        assertThat(sut.searchAdminSchedules(null, null, null, MEMBER_ID)).isEmpty();

        then(loadScheduleParticipantPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("운영진 상세는 일정 참여 조건을 검증하고 상태별 참여자를 반환한다")
    void gets_admin_schedule_for_participant() {
        Schedule schedule = identifiedSchedule(SCHEDULE_ID);
        given(loadSchedulePort.findByIdWithTags(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.of(participant(schedule)));
        given(loadScheduleParticipantPort.findParticipantDetailsByScheduleIdAndStatus(
            SCHEDULE_ID, AttendanceStatus.ABSENT)).willReturn(
                List.of(detail(SCHEDULE_ID, 2L, AttendanceStatus.ABSENT))
            );

        AdminScheduleInfo result = sut.getAdminSchedule(SCHEDULE_ID, MEMBER_ID, AttendanceStatus.ABSENT);

        assertThat(result.participants()).singleElement()
            .extracting(AdminScheduleInfo.AdminScheduleParticipantInfo::attendanceStatus)
            .isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("일정 참여자가 아니면 운영진 상세를 거부한다")
    void admin_detail_requires_participant() {
        given(loadSchedulePort.findByIdWithTags(SCHEDULE_ID))
            .willReturn(Optional.of(identifiedSchedule(SCHEDULE_ID)));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.empty());

        assertError(() -> sut.getAdminSchedule(SCHEDULE_ID, MEMBER_ID, null),
            ScheduleErrorCode.NOT_SCHEDULE_PARTICIPANT);
    }

    @Test
    @DisplayName("출석 정책 존재 여부와 일정 기본 정보를 조회한다")
    void gets_policy_and_base_info() {
        Schedule schedule = identifiedSchedule(SCHEDULE_ID);
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

        assertThat(sut.hasAttendancePolicy(SCHEDULE_ID)).isTrue();
        ScheduleBaseInfo info = sut.getScheduleBaseInfo(SCHEDULE_ID);
        assertThat(info.scheduleId()).isEqualTo(SCHEDULE_ID);

        schedule.removeAttendancePolicy();
        assertThat(sut.hasAttendancePolicy(SCHEDULE_ID)).isFalse();
    }

    @Test
    @DisplayName("정책·기본 정보 조회는 존재하지 않는 일정을 구분한다")
    void simple_queries_not_found() {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());

        assertError(() -> sut.hasAttendancePolicy(SCHEDULE_ID), ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> sut.getScheduleBaseInfo(SCHEDULE_ID), ScheduleErrorCode.SCHEDULE_NOT_FOUND);
    }

    private Schedule identifiedSchedule(Long id) {
        Schedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private ScheduleParticipantDetailDto detail(Long scheduleId, Long memberId, AttendanceStatus status) {
        return new ScheduleParticipantDetailDto(
            scheduleId, memberId, "회원", "닉네임", 1L, "학교", null, status, null, true
        );
    }

    private void prepareCurrentRoles(List<ChallengerRoleInfo> roles) {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(getGisuUseCase.getActiveGisu()).willReturn(new GisuInfo(
            1L, 1L, Instant.EPOCH, Instant.parse("2030-01-01T00:00:00Z"), true
        ));
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(roles);
    }

    private ChallengerRoleInfo role(ChallengerRoleType roleType) {
        return ChallengerRoleInfo.builder().gisuId(1L).roleType(roleType).build();
    }

    private void assertError(Runnable action, ScheduleErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
