package com.umc.product.schedule.adapter.in.web.v2.dto;

import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_ENDS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_STARTS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.schedule.adapter.in.web.v2.dto.request.CreateScheduleRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.DecideAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.EditScheduleRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ExcuseScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ScheduleAttendancePolicyRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ScheduleLocationRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.AdminScheduleInfoResponse;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleCapabilitiesResponse;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleInfoResponse;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleParticipantAttendanceInfoResponse;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.result.ScheduleParticipantAttendanceResult;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleInfo;
import com.umc.product.schedule.application.port.out.dto.ScheduleParticipantDetailDto;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@DisplayName("Schedule DTO 변환")
class ScheduleDtoTest {

    @Test
    @DisplayName("생성 request는 위치·정책·참여자를 command와 entity로 손실 없이 변환한다")
    void create_request_to_entity() {
        ScheduleLocationRequest location = new ScheduleLocationRequest(37.0, 127.0, "라운지");
        ScheduleAttendancePolicyRequest policy = policyRequest();
        CreateScheduleRequest request = new CreateScheduleRequest(
            "세션", "설명", Set.of(ScheduleTag.STUDY), DEFAULT_STARTS_AT, DEFAULT_ENDS_AT,
            location, policy, Set.of(2L, 3L)
        );

        CreateScheduleCommand command = request.toCommand(1L);
        Schedule entity = command.toEntity(10L);

        assertThat(command.authorMemberId()).isEqualTo(1L);
        assertThat(command.participantMemberIds()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(entity.getAuthorMemberId()).isEqualTo(10L);
        assertThat(entity.getLocation().getY()).isEqualTo(37.0);
        assertThat(entity.getLocation().getX()).isEqualTo(127.0);
        assertThat(entity.getLocationName()).isEqualTo("라운지");
        assertThat(entity.getPolicy()).isNotNull();
    }

    @Test
    @DisplayName("생성 request의 선택 필드가 없으면 빈 참여자와 온라인·무출석 일정으로 변환한다")
    void create_request_normalizes_optional_fields() {
        CreateScheduleRequest request = new CreateScheduleRequest(
            "세션", null, Set.of(ScheduleTag.GENERAL), DEFAULT_STARTS_AT, DEFAULT_ENDS_AT,
            null, null, null
        );

        CreateScheduleCommand command = request.toCommand(1L);
        Schedule entity = command.toEntity(1L);

        assertThat(command.participantMemberIds()).isEmpty();
        assertThat(command.location()).isNull();
        assertThat(command.attendancePolicy()).isNull();
        assertThat(entity.getLocation()).isNull();
        assertThat(entity.getPolicy()).isNull();
    }

    @Test
    @DisplayName("수정 request는 모든 선택 필드를 command로 변환한다")
    void edit_request_to_command() {
        EditScheduleRequest request = new EditScheduleRequest(
            "수정", "설명", Set.of(ScheduleTag.MEETING), DEFAULT_STARTS_AT, DEFAULT_ENDS_AT,
            new ScheduleLocationRequest(37.0, 127.0, "장소"), false,
            policyRequest(), true, Set.of(2L)
        );

        EditScheduleCommand command = request.toCommand(10L, 1L);

        assertThat(command.scheduleId()).isEqualTo(10L);
        assertThat(command.location().locationName()).isEqualTo("장소");
        assertThat(command.attendancePolicy().lateEndAt())
            .isEqualTo(DEFAULT_STARTS_AT.plus(20, ChronoUnit.MINUTES));
        assertThat(command.isParticipantsUpdateRequested()).isTrue();
        assertThat(command.isChangingToOffline()).isTrue();
        assertThat(command.isChangingToAttendanceRequired()).isTrue();
        assertThat(command.isChangingToOnline()).isFalse();
        assertThat(command.isChangingToAttendanceNotRequired()).isFalse();
        command.validate();
    }

    @Test
    @DisplayName("수정 command는 온라인·대면·출석 정책 조합의 모순을 거부한다")
    void edit_command_validates_mode_changes() {
        EditScheduleCommand offlineWithoutLocation = editCommand(false, null, null, null);
        EditScheduleCommand onlineWithLocation = editCommand(
            true, EditScheduleCommand.LocationInfo.builder().latitude(37.0).longitude(127.0).build(), null, null
        );
        EditScheduleCommand attendanceWithoutPolicy = editCommand(null, null, true, null);

        assertError(offlineWithoutLocation::validate, ScheduleErrorCode.OFFLINE_SCHEDULE_REQUIRES_LOCATION);
        assertError(onlineWithLocation::validate, ScheduleErrorCode.ONLINE_SCHEDULE_SHOULD_NOT_HAVE_LOCATION);
        assertError(attendanceWithoutPolicy::validate, ScheduleErrorCode.ATTENDANCE_POLICY_REQUIRED);
        assertThat(editCommand(null, null, false, null).isChangingToAttendanceNotRequired()).isTrue();
        assertThat(editCommand(null, null, null, null).isParticipantsUpdateRequested()).isFalse();
    }

    @Test
    @DisplayName("출석 관련 request는 경로와 현재 회원 식별자를 command에 결합한다")
    void attendance_requests_to_commands() {
        ScheduleAttendanceCommand attendance = new ScheduleAttendanceRequest(true, 37.0, 127.0)
            .toCommand(10L, 1L);
        ExcuseScheduleAttendanceCommand excuse = new ExcuseScheduleAttendanceRequest(
            false, null, null, "병원"
        ).toCommand(10L, 1L);
        DecideAttendanceCommand decision = new DecideAttendanceRequest(2L, true, "확인")
            .toCommand(10L, 1L);

        assertThat(attendance).isEqualTo(ScheduleAttendanceCommand.builder()
            .scheduleId(10L).requesterMemberId(1L).locationVerified(true)
            .latitude(37.0).longitude(127.0).build());
        assertThat(excuse.scheduleId()).isEqualTo(10L);
        assertThat(excuse.requesterMemberId()).isEqualTo(1L);
        assertThat(excuse.excuseReason()).isEqualTo("병원");
        assertThat(decision.participantMemberId()).isEqualTo(2L);
        assertThat(decision.decidedByMemberId()).isEqualTo(1L);
        assertThatThrownBy(() -> ScheduleAttendanceCommand.builder()
            .scheduleId(10L).requesterMemberId(1L).locationVerified(null).build())
            .isInstanceOf(ScheduleDomainException.class);
    }

    @Test
    @DisplayName("출석 정책 request는 null을 validation에 위임하고 정상·역전 순서를 구분한다")
    void policy_request_time_order() {
        assertThat(policyRequest().isValidTimeOrder()).isTrue();
        assertThat(new ScheduleAttendancePolicyRequest(null, DEFAULT_STARTS_AT, DEFAULT_ENDS_AT)
            .isValidTimeOrder()).isTrue();
        assertThat(new ScheduleAttendancePolicyRequest(
            DEFAULT_STARTS_AT, DEFAULT_STARTS_AT, DEFAULT_ENDS_AT
        ).isValidTimeOrder()).isFalse();
        assertThat(new ScheduleAttendancePolicyRequest(
            DEFAULT_STARTS_AT.minusSeconds(1), DEFAULT_ENDS_AT, DEFAULT_ENDS_AT
        ).isValidTimeOrder()).isFalse();
    }

    @Test
    @DisplayName("일정 query info와 일반 response는 참여 여부·내 출석·중첩 위치·정책을 계산한다")
    void schedule_info_and_response_mapping() {
        Schedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", 10L);
        List<ScheduleParticipantDetailDto> participants = List.of(
            participantDetail(10L, 1L, AttendanceStatus.PRESENT_PENDING),
            participantDetail(10L, 2L, null)
        );

        ScheduleInfo info = ScheduleInfo.from(schedule, participants, 1L);
        ScheduleInfoResponse response = ScheduleInfoResponse.from(info);

        assertThat(info.isParticipant()).isTrue();
        assertThat(info.attendanceStatus()).isEqualTo(AttendanceStatus.PRESENT_PENDING);
        assertThat(response.scheduleId()).isEqualTo(10L);
        assertThat(response.location().latitude()).isEqualTo(37.0);
        assertThat(response.attendancePolicy().checkInStartAt())
            .isEqualTo(DEFAULT_STARTS_AT.minus(10, ChronoUnit.MINUTES));
        assertThat(response.participants()).hasSize(2);
        assertThat(ScheduleInfo.from(schedule, participants, 99L).isParticipant()).isFalse();
    }

    @Test
    @DisplayName("온라인·무출석 일정의 query response는 중첩 정보를 null로 유지한다")
    void online_schedule_nested_info_is_null() {
        Schedule schedule = schedule();
        schedule.convertToOnline();
        schedule.removeAttendancePolicy();

        ScheduleInfoResponse response = ScheduleInfoResponse.from(ScheduleInfo.from(schedule, List.of(), 1L));

        assertThat(response.isOnline()).isTrue();
        assertThat(response.location()).isNull();
        assertThat(response.attendancePolicy()).isNull();
        assertThat(response.isAttendanceChecked()).isFalse();
        assertThat(ScheduleInfoResponse.ScheduleParticipantInfoResponse.from(null)).isNull();
    }

    @Test
    @DisplayName("운영진 info와 response는 참석자의 민감 출석 필드를 포함한다")
    void admin_info_and_response_mapping() {
        Schedule schedule = schedule();
        ScheduleParticipantDetailDto participant = participantDetail(
            null, 2L, AttendanceStatus.ABSENT_EXCUSE_PENDING
        );

        AdminScheduleInfo info = AdminScheduleInfo.from(schedule, List.of(participant));
        AdminScheduleInfoResponse response = AdminScheduleInfoResponse.from(info);

        assertThat(response.participants()).singleElement().satisfies(mapped -> {
            assertThat(mapped.memberId()).isEqualTo(2L);
            assertThat(mapped.attendanceStatus()).isEqualTo(AttendanceStatus.ABSENT_EXCUSE_PENDING);
            assertThat(mapped.isLocationVerified()).isTrue();
            assertThat(mapped.excuseReason()).isEqualTo("병원");
        });
    }

    @Test
    @DisplayName("출석 결과 response는 결정자 존재 여부에 따라 중첩 정보를 변환한다")
    void attendance_result_response_mapping() {
        Instant decidedAt = Instant.parse("2026-01-01T00:00:00Z");
        ScheduleParticipantAttendanceResult result = ScheduleParticipantAttendanceResult.builder()
            .latitude(37.0)
            .longitude(127.0)
            .status(AttendanceStatus.EXCUSED)
            .excuseReason("병원")
            .isPendingDecision(false)
            .hasDecisionMakerMember(true)
            .decisionMakerMemberInfo(ScheduleParticipantAttendanceResult.DecisionMakerMemberInfo.builder()
                .memberId(1L).name("관리자").nickname("admin").schoolId(10L).schoolName("학교").build())
            .decidedAt(decidedAt)
            .decisionReason("승인")
            .build();

        ScheduleParticipantAttendanceInfoResponse response =
            ScheduleParticipantAttendanceInfoResponse.from(result);

        assertThat(response.decisionMakerMemberInfo().memberId()).isEqualTo(1L);
        assertThat(response.decidedAt()).isEqualTo(decidedAt);
        assertThat(ScheduleParticipantAttendanceInfoResponse.from(null)).isNull();
        assertThat(ScheduleParticipantAttendanceInfoResponse.from(
            ScheduleParticipantAttendanceResult.builder().hasDecisionMakerMember(false).build()
        ).decisionMakerMemberInfo()).isNull();
    }

    @Test
    @DisplayName("일정 생성 capability의 모든 role별 제한을 response에 보존한다")
    void capabilities_factories_and_response() {
        List<ScheduleCapabilitiesInfo> capabilities = List.of(
            ScheduleCapabilitiesInfo.notAllowed(),
            ScheduleCapabilitiesInfo.forChallenger(),
            ScheduleCapabilitiesInfo.forSchoolAdmin(),
            ScheduleCapabilitiesInfo.forCentralMember(),
            ScheduleCapabilitiesInfo.forSchoolCore(),
            ScheduleCapabilitiesInfo.forChapterPresident(),
            ScheduleCapabilitiesInfo.forCentralCore()
        );

        assertThat(capabilities).extracting(ScheduleCapabilitiesInfo::maxParticipantCount)
            .containsExactly(0, 50, 100, 300, 100, 300, 2_000);
        assertThat(ScheduleCapabilitiesResponse.from(capabilities.get(6)))
            .satisfies(response -> {
                assertThat(response.canCreateSchedule()).isTrue();
                assertThat(response.canCreateAttendanceRequiredSchedule()).isTrue();
                assertThat(response.maxParticipantCount()).isEqualTo(2_000);
            });
    }

    @Test
    @DisplayName("ScheduleBaseInfo는 직접 변환해 일정 기본 필드를 보존한다")
    void base_info_mapping() {
        ScheduleBaseInfo info = ScheduleBaseInfo.from(schedule());

        assertThat(info.name()).isEqualTo("정기 세션");
        assertThat(info.location().locationName()).isEqualTo("UMC 라운지");
        assertThat(info.isAttendanceChecked()).isTrue();
    }

    private ScheduleAttendancePolicyRequest policyRequest() {
        return new ScheduleAttendancePolicyRequest(
            DEFAULT_STARTS_AT.minus(10, ChronoUnit.MINUTES),
            DEFAULT_STARTS_AT.plus(10, ChronoUnit.MINUTES),
            DEFAULT_STARTS_AT.plus(20, ChronoUnit.MINUTES)
        );
    }

    private EditScheduleCommand editCommand(
        Boolean isOnline,
        EditScheduleCommand.LocationInfo location,
        Boolean isAttendanceRequired,
        Set<Long> participantIds
    ) {
        return EditScheduleCommand.builder()
            .scheduleId(1L)
            .isOnline(isOnline)
            .location(location)
            .isAttendanceRequired(isAttendanceRequired)
            .participantMemberIds(participantIds)
            .build();
    }

    private ScheduleParticipantDetailDto participantDetail(
        Long scheduleId,
        Long memberId,
        AttendanceStatus status
    ) {
        return new ScheduleParticipantDetailDto(
            scheduleId, memberId, "회원" + memberId, "닉네임" + memberId,
            10L, "학교", "profile.png", status, "병원", true
        );
    }

    private void assertError(Runnable action, ScheduleErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
